import hashlib
import json
import logging
import os
import random
import re
import time
from datetime import date, datetime, time as dt_time, timedelta
from zoneinfo import ZoneInfo

import dateparser
import requests
from bs4 import BeautifulSoup
from playwright.sync_api import sync_playwright

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
    datefmt="%Y-%m-%dT%H:%M:%S",
)
log = logging.getLogger(__name__)


def _tz_now() -> datetime:
    timezone = os.getenv("SCRAPPER_TIMEZONE", "Europe/Warsaw")
    return datetime.now(ZoneInfo(timezone))


def _clean_text(value: str | None) -> str:
    if not value:
        return ""
    return " ".join(value.split())


def _parse_date_from_label(label: str) -> date | None:
    cleaned = _clean_text(label)
    if not cleaned:
        return None

    timezone = os.getenv("SCRAPPER_TIMEZONE", "Europe/Warsaw")
    parsed = dateparser.parse(
        cleaned,
        languages=["pl"],
        settings={
            "TIMEZONE": timezone,
            "TO_TIMEZONE": timezone,
            "RETURN_AS_TIMEZONE_AWARE": True,
            "PREFER_DATES_FROM": "future",
        },
    )
    if parsed:
        return parsed.date()

    # Fallback for labels like "08.05" without a year.
    match = re.search(r"(\d{1,2})\.(\d{1,2})", cleaned)
    if not match:
        return None
    day = int(match.group(1))
    month = int(match.group(2))
    year = _tz_now().year
    try:
        return date(year, month, day)
    except ValueError:
        return None


def _parse_date_from_iso(value: str | None) -> date | None:
    cleaned = _clean_text(value)
    if not cleaned:
        return None
    try:
        return date.fromisoformat(cleaned[:10])
    except ValueError:
        return None


def _parse_datetime_from_label(label: str) -> datetime | None:
    cleaned = _clean_text(label)
    if not cleaned:
        return None
    timezone = os.getenv("SCRAPPER_TIMEZONE", "Europe/Warsaw")
    return dateparser.parse(
        cleaned,
        languages=["pl"],
        settings={
            "TIMEZONE": timezone,
            "TO_TIMEZONE": timezone,
            "RETURN_AS_TIMEZONE_AWARE": True,
            "PREFER_DATES_FROM": "future",
        },
    )


def _parse_time_from_label(label: str) -> dt_time | None:
    cleaned = _clean_text(label)
    match = re.search(r"godz\.\s*(\d{1,2})[:.](\d{2})", cleaned, flags=re.IGNORECASE)
    if not match:
        return None

    hour = int(match.group(1))
    minute = int(match.group(2))
    try:
        return dt_time(hour=hour, minute=minute)
    except ValueError:
        return None


def _truncate(value: str, max_len: int) -> str:
    cleaned = _clean_text(value)
    if len(cleaned) <= max_len:
        return cleaned
    return cleaned[: max_len - 1].rstrip() + "…"


def _safe_post_key(post: dict) -> str:
    event_url = post.get("eventUrl")
    if event_url:
        return event_url
    return f"{post.get('title', '')}|{post.get('startsAt', '')}"


def _resolve_target_date(now: datetime) -> date:
    configured = os.getenv("SCRAPPER_FILTER_TARGET_DATE", "today").strip().lower()
    if configured in {"", "today"}:
        return now.date()
    return date.fromisoformat(configured)


def _filter_events_after_fetch(parsed_events: list[dict], now: datetime) -> list[dict]:
    source = os.getenv("SCRAPPER_FILTER_DATE_SOURCE", "event_date").strip().lower()
    target_date = _resolve_target_date(now)

    if source == "event_date":
        return [e for e in parsed_events if e.get("event_date") == target_date.isoformat()]
    if source == "fetched_at":
        return list(parsed_events)
    raise RuntimeError("SCRAPPER_FILTER_DATE_SOURCE must be 'event_date' or 'fetched_at'")


def _iso_utc(dt: datetime) -> str:
    return dt.astimezone(ZoneInfo("UTC")).isoformat().replace("+00:00", "Z")


def _build_post_payload(event: dict, now: datetime) -> dict | None:
    event_day = _parse_date_from_iso(event.get("event_date"))
    if event_day is not None:
        event_time = _parse_time_from_label(event.get("date_label", "")) or dt_time(hour=12)
        event_dt = datetime.combine(
            event_day,
            event_time,
            tzinfo=ZoneInfo(os.getenv("SCRAPPER_TIMEZONE", "Europe/Warsaw")),
        )
    else:
        event_dt = _parse_datetime_from_label(event.get("date_label", ""))
        if event_dt is None:
            event_day = _parse_date_from_label(event.get("date_label", ""))
            if event_day is None:
                return None
            event_dt = datetime.combine(
                event_day,
                dt_time(hour=12, minute=0),
                tzinfo=ZoneInfo(os.getenv("SCRAPPER_TIMEZONE", "Europe/Warsaw")),
            )

    if event_dt <= now:
        event_day = _parse_date_from_label(event.get("date_label", ""))
        if event_day is None:
            event_day = _parse_date_from_iso(event.get("event_date"))
        if event_day == now.date():
            # Same-day event whose listed time has passed — schedule near-future so @Future passes.
            event_dt = now + timedelta(minutes=5)
        else:
            return None

    title = _truncate(event.get("name", "Unknown event"), 32)
    description_parts = [
        event.get("name", ""),
        f"Kiedy: {event.get('date_label') or 'Nieznane'}",
        f"Gdzie: {event.get('where') or 'Nieznane'}",
        f"Cena: {event.get('price') if event.get('price') and event.get('price') != 'Check website' else 'Sprawdź stronę'}",
        f"Źródło: {event.get('source_url') or 'https://www.trojmiasto.pl'}",
    ]
    description = _truncate(" | ".join(filter(None, description_parts)), 1024)
    tags = [_truncate("trojmiasto", 32), _truncate("scrapper", 32)]

    return {
        "title": title,
        "description": description,
        "eventUrl": event.get("source_url"),
        "startsAt": _iso_utc(event_dt),
        "tags": tags,
        "type": "ONLINE",
        "visibility": "PUBLIC",
    }


def _wait_for_service(url: str, label: str, max_wait_s: int = 300, interval: int = 10) -> None:
    """Polls `url` until it returns a non-5xx response or `max_wait_s` elapses."""
    deadline = time.time() + max_wait_s
    log.info("Waiting for %s at %s", label, url)
    while True:
        try:
            resp = requests.get(url, timeout=5)
            if resp.status_code < 500:
                log.info("%s is ready (HTTP %s)", label, resp.status_code)
                return
        except requests.exceptions.RequestException as exc:
            log.debug("%s not yet reachable: %s", label, exc)

        remaining = deadline - time.time()
        if remaining <= 0:
            raise RuntimeError(f"{label} did not become ready within {max_wait_s}s")

        sleep_for = min(interval, remaining)
        log.info("%s not ready, retrying in %.0fs…", label, sleep_for)
        time.sleep(sleep_for)


def _wait_for_services() -> None:
    """Blocks until Keycloak (and REST API when ingestion is enabled) are healthy."""
    if os.getenv("SCRAPPER_INGEST_ENABLED", "true").lower() != "true":
        return

    keycloak_base = os.getenv("SCRAPPER_KEYCLOAK_BASE_URL", "http://keycloak:8180")
    realm = os.getenv("SCRAPPER_KEYCLOAK_REALM", "coolture-dev")
    _wait_for_service(
        f"{keycloak_base}/realms/{realm}/.well-known/openid-configuration",
        "Keycloak",
    )

    api_base = os.getenv("SCRAPPER_API_BASE_URL", "http://rest-api:8081/api").rstrip("/")
    _wait_for_service(f"{api_base}/actuator/health", "REST API")


def _get_access_token() -> str:
    keycloak_base = os.getenv("SCRAPPER_KEYCLOAK_BASE_URL", "http://keycloak:8180")
    realm = os.getenv("SCRAPPER_KEYCLOAK_REALM", "coolture-dev")
    token_url = f"{keycloak_base}/realms/{realm}/protocol/openid-connect/token"

    grant_type = os.getenv("SCRAPPER_KEYCLOAK_GRANT_TYPE", "client_credentials").strip()
    client_id = os.getenv("SCRAPPER_KEYCLOAK_CLIENT_ID", "coolture-bot")
    client_secret = os.getenv("SCRAPPER_KEYCLOAK_CLIENT_SECRET", "")

    if grant_type == "client_credentials":
        if not client_secret:
            raise RuntimeError(
                "SCRAPPER_KEYCLOAK_CLIENT_SECRET must be set for client_credentials grant"
            )
        data = {
            "grant_type": "client_credentials",
            "client_id": client_id,
            "client_secret": client_secret,
        }
    elif grant_type == "password":
        username = os.getenv("SCRAPPER_KEYCLOAK_USERNAME", "coolture_admin")
        password = os.getenv("SCRAPPER_KEYCLOAK_PASSWORD", "admin")
        data = {
            "grant_type": "password",
            "client_id": client_id,
            "username": username,
            "password": password,
            "scope": "openid profile",
        }
        if client_secret:
            data["client_secret"] = client_secret
    else:
        raise RuntimeError(
            "SCRAPPER_KEYCLOAK_GRANT_TYPE must be 'client_credentials' or 'password'"
        )

    resp = requests.post(token_url, data=data, timeout=20)
    resp.raise_for_status()
    token = resp.json().get("access_token")
    if not token:
        raise RuntimeError("Missing access_token in Keycloak response")
    log.info("Acquired access token (grant=%s, client=%s)", grant_type, client_id)
    return token


def _resolve_category_id(api_base_url: str, token: str | None = None) -> str:
    """Returns the UUID of the target event category, creating the fallback one if needed.

    The GET on /dicts/** is public. The fallback POST uses auth when provided
    because /dicts/admin/** is intentionally protected.
    """
    preferred_name = os.getenv("SCRAPPER_EVENT_CATEGORY_NAME", "").strip().lower()
    fallback_name = os.getenv("SCRAPPER_FALLBACK_CATEGORY_NAME", "other")

    resp = requests.get(f"{api_base_url}/dicts/event-categories", timeout=20)
    resp.raise_for_status()
    categories = resp.json()

    if preferred_name:
        for cat in categories:
            if str(cat.get("name", "")).strip().lower() == preferred_name:
                log.info("Using category '%s' (id=%s)", cat["name"], cat["id"])
                return cat["id"]
        log.warning(
            "Preferred category '%s' not found; falling back to create '%s'",
            preferred_name,
            fallback_name,
        )

    if categories and not preferred_name:
        log.info("Using first available category '%s' (id=%s)", categories[0]["name"], categories[0]["id"])
        return categories[0]["id"]

    # No usable category — create the fallback via the protected admin endpoint.
    create_resp = requests.post(
        f"{api_base_url}/dicts/admin/event-categories",
        headers={
            **({"Authorization": f"Bearer {token}"} if token else {}),
            "Content-Type": "application/json",
        },
        data=json.dumps({"name": fallback_name}),
        timeout=20,
    )
    create_resp.raise_for_status()
    created = create_resp.json()
    log.info("Created fallback category '%s' (id=%s)", fallback_name, created["id"])
    return created["id"]


def _list_existing_post_keys(api_base_url: str, token: str, target_date_iso: str) -> set[str]:
    """Fetches keys of posts that already exist for `target_date_iso` to skip duplicates."""
    timezone = ZoneInfo(os.getenv("SCRAPPER_TIMEZONE", "Europe/Warsaw"))
    target_date = date.fromisoformat(target_date_iso)
    day_start = datetime.combine(target_date, dt_time.min, tzinfo=timezone)
    day_end = datetime.combine(target_date, dt_time.max, tzinfo=timezone)

    headers = {"Authorization": f"Bearer {token}"}
    base_params = {
        "limit": 100,
        "startsFrom": _iso_utc(day_start),
        "startsTo": _iso_utc(day_end),
    }

    keys: set[str] = set()
    cursor: str | None = None
    page_num = 0
    while True:
        page_num += 1
        params = {**base_params, **({"cursor": cursor} if cursor else {})}

        resp = requests.get(
            f"{api_base_url}/posts",
            headers=headers,
            params=params,
            timeout=30,
        )
        resp.raise_for_status()
        data = resp.json()

        for item in data.get("items", []):
            keys.add(_safe_post_key(item))

        page_meta = data.get("page", {})
        if not page_meta.get("hasMore"):
            break
        cursor = page_meta.get("nextCursor")
        if not cursor or page_num >= 20:
            if page_num >= 20:
                log.warning("Reached page cap while listing existing posts; stopping early")
            break

    log.info("Found %d existing post keys for %s", len(keys), target_date_iso)
    return keys


def _ingest_today_events(events: list[dict], today_iso: str, now: datetime) -> dict:
    api_base_url = os.getenv("SCRAPPER_API_BASE_URL", "http://rest-api:8081/api").rstrip("/")

    token = _get_access_token()
    existing_keys = _list_existing_post_keys(api_base_url, token, today_iso)

    inserted = 0
    failed = 0
    skipped_duplicate = 0
    skipped_past_or_invalid = 0

    for event in events:
        payload = _build_post_payload(event, now)
        if payload is None:
            skipped_past_or_invalid += 1
            continue

        event_key = (
            payload.get("eventUrl")
            or f"{payload.get('title', '')}|{payload.get('startsAt', '')}"
        )
        if event_key in existing_keys:
            skipped_duplicate += 1
            continue

        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
        }
        resp = requests.post(
            f"{api_base_url}/posts",
            headers=headers,
            data=json.dumps(payload),
            timeout=30,
        )

        if resp.status_code == 401:
            # Token expired mid-batch — refresh once and retry.
            log.warning("Token expired (401) — refreshing and retrying")
            token = _get_access_token()
            headers["Authorization"] = f"Bearer {token}"
            resp = requests.post(
                f"{api_base_url}/posts",
                headers=headers,
                data=json.dumps(payload),
                timeout=30,
            )

        if resp.status_code == 201:
            inserted += 1
            existing_keys.add(event_key)
        else:
            failed += 1
            log.warning(
                "Insert failed status=%s event=%r body=%s",
                resp.status_code,
                event.get("name", ""),
                resp.text[:400],
            )

    log.info(
        "Ingest complete: inserted=%d failed=%d duplicate=%d invalid=%d",
        inserted,
        failed,
        skipped_duplicate,
        skipped_past_or_invalid,
    )
    return {
        "inserted": inserted,
        "failed": failed,
        "skipped_duplicate": skipped_duplicate,
        "skipped_past_or_invalid": skipped_past_or_invalid,
    }


# ---------------------------------------------------------------------------
# Playwright scraping
# ---------------------------------------------------------------------------

def parse_event(html_content: str) -> dict:
    soup = BeautifulSoup(html_content, "html.parser")

    name_el = soup.select_one(".event__item__title")
    price_el = soup.select_one(".event__price__info, .event__price__free")
    date_el = soup.select_one(".event__item__date")
    location_el = soup.select_one(".event__item__location")
    link_el = soup.select_one(".event__item__title[href]")

    raw_date = _clean_text(date_el.get_text(" ", strip=True) if date_el else "")
    date_content = _clean_text(date_el.get("content") if date_el else "")
    parsed_date = _parse_date_from_iso(date_content) or _parse_date_from_label(raw_date)
    date_label = _clean_text(f"{date_content} {raw_date}") or "No date"
    href = link_el.get("href", "") if link_el else ""

    return {
        "name": _clean_text(name_el.get_text(strip=True) if name_el else "Unknown"),
        "date_label": date_label,
        "event_date": parsed_date.isoformat() if parsed_date else None,
        "price": _clean_text(price_el.get_text(strip=True) if price_el else "Check website"),
        "where": _clean_text(
            location_el.get_text(" ", strip=True) if location_el else "No location"
        ),
        "source_url": (
            href
            if href.startswith("http")
            else f"https://www.trojmiasto.pl{href}"
            if href
            else None
        ),
    }


def _collect_event_html() -> list[str]:
    target_url = os.getenv(
        "SCRAPPER_TARGET_URL",
        "https://www.trojmiasto.pl/imprezy/kalendarz-imprez/dni,30dni.html",
    )
    max_scrolls = int(os.getenv("SCRAPPER_MAX_SCROLLS", "150"))
    scroll_pause = float(os.getenv("SCRAPPER_SCROLL_PAUSE_S", "1.2"))

    log.info("Launching browser for %s", target_url)
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        page = browser.new_page()
        page.goto(target_url, wait_until="networkidle", timeout=60_000)

        last_count = 0
        scrolls = 0
        while scrolls < max_scrolls:
            page.keyboard.press("End")
            time.sleep(scroll_pause)
            scrolls += 1
            current = page.query_selector_all(".event__item")
            count = len(current)
            if count == last_count:
                log.info("Scroll stabilised at %d events after %d scrolls", count, scrolls)
                break
            last_count = count
            if scrolls % 20 == 0:
                log.info("Scrolled %d times, %d events so far", scrolls, count)

        event_htmls = [el.inner_html() for el in page.query_selector_all(".event__item")]
        browser.close()

    log.info("Scraped %d raw event blocks (%d scrolls)", len(event_htmls), scrolls)
    return event_htmls


def _write_dump_if_enabled(payload: dict) -> str | None:
    if os.getenv("SCRAPPER_WRITE_DUMP", "false").lower() != "true":
        return None

    output_dir = os.getenv("SCRAPPER_OUTPUT_DIR", "/tmp/scrapper")
    os.makedirs(output_dir, exist_ok=True)

    today_value = payload["date"]
    timestamp = payload["generated_at"].replace(":", "-")
    output_file = f"{output_dir}/trojmiasto-events-{today_value}-{timestamp}.json"
    latest_file = f"{output_dir}/latest.json"

    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(payload, f, indent=2, ensure_ascii=True)
    with open(latest_file, "w", encoding="utf-8") as f:
        json.dump(payload, f, indent=2, ensure_ascii=True)
    return output_file


# ---------------------------------------------------------------------------
# Orchestration
# ---------------------------------------------------------------------------

def scrape_today_events() -> dict:
    start_time = time.time()
    now = _tz_now()
    target_date = _resolve_target_date(now)
    log.info("Scrape run starting for %s", target_date.isoformat())

    event_htmls = _collect_event_html()

    log.info("Parsing %d event HTML blocks", len(event_htmls))
    parsed_events = [parse_event(html) for html in event_htmls]

    today_events = _filter_events_after_fetch(parsed_events, now)
    log.info(
        "Filtered to %d events for %s (total scraped: %d)",
        len(today_events),
        target_date.isoformat(),
        len(parsed_events),
    )

    ingest_enabled = os.getenv("SCRAPPER_INGEST_ENABLED", "true").lower() == "true"
    ingest_stats = None
    if ingest_enabled:
        ingest_stats = _ingest_today_events(today_events, target_date.isoformat(), now)

    payload = {
        "source": "trojmiasto.pl",
        "date": target_date.isoformat(),
        "filter_date_source": os.getenv("SCRAPPER_FILTER_DATE_SOURCE", "event_date"),
        "generated_at": now.isoformat(),
        "total_scraped": len(parsed_events),
        "total_today": len(today_events),
        "ingest_enabled": ingest_enabled,
        "ingest": ingest_stats,
        "events": today_events,
    }

    output_file = _write_dump_if_enabled(payload)
    duration = time.time() - start_time
    if output_file:
        log.info(
            "Run complete: %d events → %s (%.1fs)",
            len(today_events),
            output_file,
            duration,
        )
    else:
        log.info(
            "Run complete: %d events for %s (%.1fs)",
            len(today_events),
            target_date.isoformat(),
            duration,
        )
    return payload


def _daily_run_datetime(day: date) -> datetime:
    tz = ZoneInfo(os.getenv("SCRAPPER_TIMEZONE", "Europe/Warsaw"))
    start_hour = int(os.getenv("SCRAPPER_SCHEDULE_WINDOW_START_HOUR", "6"))
    end_hour = int(os.getenv("SCRAPPER_SCHEDULE_WINDOW_END_HOUR", "22"))
    if end_hour <= start_hour:
        raise ValueError("SCRAPPER_SCHEDULE_WINDOW_END_HOUR must be greater than start hour")

    salt = os.getenv("SCRAPPER_SCHEDULE_SALT", "coolture-scrapper")
    digest = hashlib.sha256(f"{salt}:{day.isoformat()}".encode()).hexdigest()
    rng = random.Random(int(digest[:8], 16))
    hour = rng.randint(start_hour, end_hour - 1)
    minute = rng.randint(0, 59)
    return datetime.combine(day, dt_time(hour=hour, minute=minute), tzinfo=tz)


def run_scheduler() -> None:
    _wait_for_services()

    if os.getenv("SCRAPPER_RUN_ON_START", "true").lower() == "true":
        try:
            scrape_today_events()
        except Exception:
            log.exception("Startup scrape failed — will retry at next scheduled time")

    while True:
        now = _tz_now()
        run_at = _daily_run_datetime(now.date())
        if run_at <= now:
            run_at = _daily_run_datetime((now + timedelta(days=1)).date())

        sleep_seconds = max((run_at - now).total_seconds(), 0)
        log.info("Next run scheduled at %s (in %ds)", run_at.isoformat(), int(sleep_seconds))

        while sleep_seconds > 0:
            chunk = min(sleep_seconds, 300)
            time.sleep(chunk)
            sleep_seconds -= chunk

        try:
            scrape_today_events()
        except Exception:
            log.exception("Scheduled scrape failed — will retry at next scheduled slot")


if __name__ == "__main__":
    mode = os.getenv("SCRAPPER_MODE", "scheduler").lower()
    if mode == "once":
        _wait_for_services()
        scrape_today_events()
    elif mode == "scheduler":
        run_scheduler()
    else:
        raise ValueError(f"SCRAPPER_MODE must be 'once' or 'scheduler', got '{mode}'")
