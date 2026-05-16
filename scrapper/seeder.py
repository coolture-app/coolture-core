"""
Seeds the rest-api country_codes dictionary table.

Runs once on scrapper container startup, before 3city.py (see entrypoint.sh).

Event categories are intentionally NOT seeded — they were made optional on
Post, so the scrapper either resolves an existing category (if some admin
created one) or omits categoryId entirely. The category endpoints still
exist for admins to manage on demand.

Authenticates with the same Keycloak service account that 3city.py uses
(client_credentials grant against coolture-bot). The bot must hold
ROLE_COOLTURE_ADMIN to POST to /dicts/admin/**.

Idempotent: existing entries are detected via the public GET endpoint and
skipped, so re-running the seeder on every container start is safe.
"""

import json
import logging
import os
import time

import requests


DEFAULT_COUNTRY_CODES = [
    "POL", "DEU", "CZE", "SVK", "UKR", "LTU", "BLR",
    "USA", "GBR", "FRA", "ITA", "ESP", "NLD",
]


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s [seeder] %(message)s",
    datefmt="%Y-%m-%dT%H:%M:%S",
)
log = logging.getLogger(__name__)


def _ingest_enabled() -> bool:
    return os.getenv("SCRAPPER_INGEST_ENABLED", "true").lower() == "true"


def _wait_for_service(url: str, label: str, max_wait_s: int = 300, interval: int = 10) -> None:
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


def _get_access_token() -> str:
    keycloak_base = os.getenv("SCRAPPER_KEYCLOAK_BASE_URL", "http://keycloak:8180")
    realm = os.getenv("SCRAPPER_KEYCLOAK_REALM", "coolture-dev")
    client_id = os.getenv("SCRAPPER_KEYCLOAK_CLIENT_ID", "coolture-bot")
    client_secret = os.getenv("SCRAPPER_KEYCLOAK_CLIENT_SECRET", "")

    if not client_secret:
        raise RuntimeError(
            "SCRAPPER_KEYCLOAK_CLIENT_SECRET must be set for the seeder"
        )

    resp = requests.post(
        f"{keycloak_base}/realms/{realm}/protocol/openid-connect/token",
        data={
            "grant_type": "client_credentials",
            "client_id": client_id,
            "client_secret": client_secret,
        },
        timeout=20,
    )
    resp.raise_for_status()
    token = resp.json().get("access_token")
    if not token:
        raise RuntimeError("Missing access_token in Keycloak response")
    return token


def _get_existing(api_base_url: str, public_path: str, key_field: str) -> set:
    """Returns the set of existing keys from a public read endpoint."""
    resp = requests.get(f"{api_base_url}/{public_path}", timeout=20)
    resp.raise_for_status()
    return {item[key_field] for item in resp.json()}


def _post_one(api_base_url: str, admin_path: str, token: str, body: dict, label: str) -> bool:
    """POSTs one row to an admin endpoint.

    Returns True on 201, False on 409 (race with another seeder run).
    Raises on any other status.
    """
    resp = requests.post(
        f"{api_base_url}/{admin_path}",
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
        },
        data=json.dumps(body),
        timeout=20,
    )
    if resp.status_code == 201:
        log.info("seeded %s: %s", label, body)
        return True
    if resp.status_code == 409:
        # Concurrent seeder run won the race; safe to ignore.
        log.info("skip %s (already exists): %s", label, body)
        return False
    raise RuntimeError(
        f"failed to seed {label}={body}: status={resp.status_code} body={resp.text[:200]}"
    )


def seed_country_codes(api_base_url: str, token: str, codes: list) -> int:
    existing = _get_existing(api_base_url, "dicts/country-codes", "code")
    added = 0
    for code in codes:
        if code in existing:
            continue
        if _post_one(api_base_url, "dicts/admin/country-codes", token, {"code": code}, "country code"):
            added += 1
    return added


def _wait_for_dependencies(api_base_url: str) -> None:
    keycloak_base = os.getenv("SCRAPPER_KEYCLOAK_BASE_URL", "http://keycloak:8180")
    realm = os.getenv("SCRAPPER_KEYCLOAK_REALM", "coolture-dev")
    _wait_for_service(
        f"{keycloak_base}/realms/{realm}/.well-known/openid-configuration",
        "Keycloak",
    )
    _wait_for_service(f"{api_base_url}/actuator/health", "REST API")


def seed() -> dict:
    if not _ingest_enabled():
        log.info("Seed skipped because SCRAPPER_INGEST_ENABLED=false")
        return {"codes_added": 0, "skipped": True}

    api_base_url = os.getenv("SCRAPPER_API_BASE_URL", "http://rest-api:8081/api").rstrip("/")

    _wait_for_dependencies(api_base_url)
    token = _get_access_token()

    codes_added = seed_country_codes(api_base_url, token, DEFAULT_COUNTRY_CODES)

    log.info("Seed complete: %d new country codes", codes_added)
    return {"codes_added": codes_added}


if __name__ == "__main__":
    seed()
