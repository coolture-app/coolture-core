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

        payload = threecity._build_post_payload(event, now)

        self.assertIsNotNone(payload)
        self.assertEqual(payload["startsAt"], "2026-06-05T17:00:00Z")
        # Base payloads default to ONLINE until enrichment attaches a location.
        self.assertEqual(payload["type"], "ONLINE")


class TestParseEventEnrichmentFields(unittest.TestCase):
    def test_extracts_city_venue_and_thumbnail(self):
        html = """
        <div class="event__item">
          <div class="event__item__img"><a href="#">
            <img itemprop="image" src="https://s-trojmiasto.pl/zdj/x/185x185/1.webp"/>
          </a></div>
          <a class="event__item__title" href="/imprezy/Test-imp1.html">Test event</a>
          <div class="event__item__location">
            <span class="event__item__location__city">Gdańsk, </span>
            <a class="event__item__location__place">Filharmonia Bałtycka</a>
          </div>
        </div>
        """

        event = threecity.parse_event(html)

        self.assertEqual(event["city"], "Gdańsk")
        self.assertEqual(event["venue"], "Filharmonia Bałtycka")
        self.assertEqual(
            event["thumb_image_url"], "https://s-trojmiasto.pl/zdj/x/185x185/1.webp"
        )


class TestFetchEventDetails(unittest.TestCase):
    DETAIL_HTML = """
    <html><head>
      <meta property="og:image" content="https://s-trojmiasto.pl/zdj/1200x0/4249151.webp"/>
      <script type="application/ld+json">
      {"@context":"https://schema.org","@type":"Event","name":"Pat Metheny",
       "image":"https://s-trojmiasto.pl/zdj/600x0/4249151.webp",
       "eventAttendanceMode":"https://schema.org/OfflineEventAttendanceMode",
       "location":{"@type":"Place","name":"Polska Filharmonia Bałtycka",
         "address":{"@type":"PostalAddress","streetAddress":"Ołowianka 1",
           "addressCountry":"PL","postalCode":"80-751","addressLocality":"Gdańsk"}}}
      </script>
    </head><body></body></html>
    """

    @patch("threecity.requests.get")
    def test_parses_image_and_address_from_jsonld(self, mock_get):
        mock_get.return_value = _resp(200, text=self.DETAIL_HTML)

        details = threecity.fetch_event_details("https://www.trojmiasto.pl/imprezy/x.html")

        self.assertEqual(
            details["image_url"], "https://s-trojmiasto.pl/zdj/1200x0/4249151.webp"
        )
        self.assertEqual(details["postal_code"], "80-751")
        self.assertEqual(details["city"], "Gdańsk")
        self.assertEqual(details["street"], "Ołowianka 1")
        self.assertEqual(details["venue"], "Polska Filharmonia Bałtycka")
        self.assertEqual(details["country_code"], "PL")
        self.assertFalse(details["is_online"])

    def test_missing_url_returns_empty_details(self):
        details = threecity.fetch_event_details(None)
        self.assertIsNone(details["image_url"])
        self.assertFalse(details["is_online"])


class TestBuildLocationPayload(unittest.TestCase):
    NOMINATIM_HIT = {
        "lat": "54.3520",
        "lon": "18.6466",
        "address": {
            "road": "Ołowianka",
            "house_number": "1",
            "city": "Gdańsk",
            "postcode": "80-751",
            "country_code": "pl",
        },
    }

    @patch("threecity._nominatim_search")
    def test_builds_offline_location_from_geocode(self, mock_search):
        mock_search.return_value = self.NOMINATIM_HIT
        event = {"city": "Gdańsk", "venue": "Filharmonia Bałtycka"}
        details = {
            "is_online": False,
            "city": "Gdańsk",
            "street": "Ołowianka 1",
            "postal_code": "80-751",
            "venue": "Polska Filharmonia Bałtycka",
        }

        loc = threecity.build_location_payload(event, details)

        self.assertEqual(loc["countryCode"], "POL")
        self.assertEqual(loc["city"], "Gdańsk")
        self.assertEqual(loc["postalCode"], "80-751")
        self.assertEqual(loc["coordinates"]["latitude"], 54.3520)
        self.assertEqual(loc["coordinates"]["longitude"], 18.6466)
        self.assertEqual(loc["venueName"], "Polska Filharmonia Bałtycka")

    @patch("threecity._nominatim_search")
    def test_online_event_has_no_location(self, mock_search):
        loc = threecity.build_location_payload({}, {"is_online": True})
        self.assertIsNone(loc)
        mock_search.assert_not_called()

    @patch("threecity._nominatim_search", return_value=None)
    def test_ungeocodable_event_returns_none(self, _mock_search):
        loc = threecity.build_location_payload({"city": "Nowhere"}, {"is_online": False})
        self.assertIsNone(loc)

    @patch("threecity._nominatim_search")
    def test_falls_back_to_city_postcode_when_missing(self, mock_search):
        mock_search.return_value = {
            "lat": "54.35", "lon": "18.64", "address": {"country_code": "pl"}
        }
        loc = threecity.build_location_payload(
            {"city": "Gdańsk"}, {"is_online": False, "city": "Gdańsk"}
        )
        self.assertEqual(loc["postalCode"], "80-001")


class TestUploadCoverImage(unittest.TestCase):
    @patch("threecity.requests.post")
    @patch("threecity.requests.get")
    def test_uploads_downloaded_image_and_returns_media_id(self, mock_get, mock_post):
        img = _resp(200, text="")
        img.content = b"\x89PNG\r\n"
        img.headers = {"Content-Type": "image/png"}
        mock_get.return_value = img
        mock_post.return_value = _resp(201, json_data={"id": "media-123"})

        media_id = threecity.upload_cover_image(
            "http://api", "token", "https://s-trojmiasto.pl/img/x.png"
        )

        self.assertEqual(media_id, "media-123")
        _, kwargs = mock_post.call_args
        self.assertEqual(kwargs["params"]["purpose"], "EVENT_COVER")
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer token")
        self.assertIn("file", kwargs["files"])

    @patch("threecity.requests.get")
    def test_unsupported_mime_is_skipped(self, mock_get):
        img = _resp(200, text="")
        img.content = b"GIF89a"
        img.headers = {"Content-Type": "image/gif"}
        mock_get.return_value = img

        media_id = threecity.upload_cover_image(
            "http://api", "token", "https://s-trojmiasto.pl/img/x.gif"
        )
        self.assertIsNone(media_id)

    def test_no_image_url_returns_none(self):
        self.assertIsNone(threecity.upload_cover_image("http://api", "token", None))


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
