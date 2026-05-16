"""Unit tests for scrapper/3city.py helper behavior."""

import importlib.util
import os
import sys
import types
import unittest
from datetime import datetime
from pathlib import Path
from unittest.mock import MagicMock, patch
from zoneinfo import ZoneInfo

HERE = Path(__file__).resolve().parent
SCRAPPER_DIR = HERE.parent


def _load_scrapper_module():
    dateparser = types.ModuleType("dateparser")
    dateparser.parse = lambda *args, **kwargs: None
    sys.modules["dateparser"] = dateparser

    sync_api = types.ModuleType("playwright.sync_api")
    sync_api.sync_playwright = lambda: None
    playwright = types.ModuleType("playwright")
    playwright.sync_api = sync_api
    sys.modules["playwright"] = playwright
    sys.modules["playwright.sync_api"] = sync_api

    spec = importlib.util.spec_from_file_location("threecity", SCRAPPER_DIR / "3city.py")
    module = importlib.util.module_from_spec(spec)
    sys.modules["threecity"] = module
    spec.loader.exec_module(module)
    return module


threecity = _load_scrapper_module()


def _resp(status_code: int, json_data=None, text: str = ""):
    response = MagicMock()
    response.status_code = status_code
    response.json.return_value = json_data if json_data is not None else {}
    response.text = text
    response.raise_for_status = MagicMock()
    return response


class TestParseEvent(unittest.TestCase):
    def test_uses_start_date_content_attribute_and_visible_time(self):
        html = """
        <div class="event__item">
          <a class="event__item__title" href="/imprezy/Test-imp1.html">Test event</a>
          <p itemprop="startDate" content="2026-06-05" class="event__item__date">
            piątek, <span class="event__item__date__hour">godz. <strong>19:00</strong></span>
          </p>
          <div class="event__item__location">
            <span class="event__item__location__city">Gdańsk, </span>
            <span class="event__item__location__address">Wyspa Sobieszewska</span>
          </div>
          <span class="event__price__free">Wstęp wolny</span>
        </div>
        """

        event = threecity.parse_event(html)

        self.assertEqual(event["name"], "Test event")
        self.assertEqual(event["event_date"], "2026-06-05")
        self.assertEqual(event["date_label"], "2026-06-05 piątek, godz. 19:00")
        self.assertEqual(event["price"], "Wstęp wolny")
        self.assertEqual(event["where"], "Gdańsk, Wyspa Sobieszewska")
        self.assertEqual(
            event["source_url"],
            "https://www.trojmiasto.pl/imprezy/Test-imp1.html",
        )


class TestBuildPostPayload(unittest.TestCase):
    @patch.dict(os.environ, {"SCRAPPER_TIMEZONE": "Europe/Warsaw"}, clear=False)
    def test_builds_starts_at_from_event_date_and_visible_time(self):
        now = datetime(2026, 5, 16, 12, tzinfo=ZoneInfo("Europe/Warsaw"))
        event = {
            "name": "Test event",
            "date_label": "2026-06-05 piątek, godz. 19:00",
            "event_date": "2026-06-05",
            "source_url": "https://www.trojmiasto.pl/imprezy/Test-imp1.html",
            "where": "Gdańsk",
            "price": "Wstęp wolny",
        }

        payload = threecity._build_post_payload(event, "category-id", now)

        self.assertIsNotNone(payload)
        self.assertEqual(payload["startsAt"], "2026-06-05T17:00:00Z")


class TestResolveCategoryId(unittest.TestCase):
    @patch("threecity.requests.post")
    @patch("threecity.requests.get")
    def test_fallback_category_creation_uses_bearer_token(self, mock_get, mock_post):
        mock_get.return_value = _resp(200, json_data=[])
        mock_post.return_value = _resp(201, json_data={"id": "cat-id", "name": "other"})

        category_id = threecity._resolve_category_id("http://api", "token")

        self.assertEqual(category_id, "cat-id")
        _, kwargs = mock_post.call_args
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer token")


if __name__ == "__main__":
    unittest.main()
