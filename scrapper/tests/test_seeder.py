"""Unit tests for scrapper/seeder.py.

Run from the scrapper/ directory:
    python -m unittest discover -s tests -v
"""

import os
import sys
import unittest
from unittest.mock import MagicMock, patch

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))

import seeder


def _resp(status_code: int, json_data=None, text: str = ""):
    """Builds a fake requests.Response-like object."""
    r = MagicMock()
    r.status_code = status_code
    r.json.return_value = json_data if json_data is not None else {}
    r.text = text
    r.raise_for_status = MagicMock()
    return r


class TestGetExisting(unittest.TestCase):
    @patch("seeder.requests.get")
    def test_returns_set_of_keys(self, mock_get):
        mock_get.return_value = _resp(
            200,
            json_data=[{"name": "other"}, {"name": "concert"}, {"name": "film"}],
        )
        existing = seeder._get_existing("http://api/api", "dicts/event-categories", "name")
        self.assertEqual(existing, {"other", "concert", "film"})

    @patch("seeder.requests.get")
    def test_returns_empty_set_when_no_rows(self, mock_get):
        mock_get.return_value = _resp(200, json_data=[])
        self.assertEqual(
            seeder._get_existing("http://api/api", "dicts/country-codes", "code"), set()
        )


class TestPostOne(unittest.TestCase):
    @patch("seeder.requests.post")
    def test_returns_true_on_201(self, mock_post):
        mock_post.return_value = _resp(201, json_data={"id": "abc"})
        result = seeder._post_one(
            "http://api/api", "dicts/admin/event-categories", "tok", {"name": "x"}, "category"
        )
        self.assertTrue(result)
        # auth header attached
        _, kwargs = mock_post.call_args
        self.assertEqual(kwargs["headers"]["Authorization"], "Bearer tok")

    @patch("seeder.requests.post")
    def test_returns_false_on_409(self, mock_post):
        mock_post.return_value = _resp(409, text="already exists")
        result = seeder._post_one(
            "http://api/api", "dicts/admin/event-categories", "tok", {"name": "x"}, "category"
        )
        self.assertFalse(result)

    @patch("seeder.requests.post")
    def test_raises_on_403(self, mock_post):
        """If the bot doesn't have admin role, POST returns 403 — must fail loudly."""
        mock_post.return_value = _resp(403, text="forbidden")
        with self.assertRaises(RuntimeError) as ctx:
            seeder._post_one(
                "http://api/api", "dicts/admin/event-categories", "tok", {"name": "x"}, "category"
            )
        self.assertIn("403", str(ctx.exception))

    @patch("seeder.requests.post")
    def test_raises_on_500(self, mock_post):
        mock_post.return_value = _resp(500, text="boom")
        with self.assertRaises(RuntimeError):
            seeder._post_one(
                "http://api/api", "dicts/admin/event-categories", "tok", {"name": "x"}, "category"
            )


class TestSeedCountryCodes(unittest.TestCase):
    @patch("seeder._post_one")
    @patch("seeder._get_existing")
    def test_only_posts_missing(self, mock_existing, mock_post):
        mock_existing.return_value = {"POL", "DEU"}
        mock_post.return_value = True
        added = seeder.seed_country_codes("http://api/api", "tok", ["POL", "DEU", "USA"])
        self.assertEqual(added, 1)
        posted_bodies = [call.args[3] for call in mock_post.call_args_list]
        self.assertEqual(posted_bodies, [{"code": "USA"}])

    @patch("seeder._post_one")
    @patch("seeder._get_existing")
    def test_admin_path_used(self, mock_existing, mock_post):
        mock_existing.return_value = set()
        mock_post.return_value = True
        seeder.seed_country_codes("http://api/api", "tok", ["POL"])
        admin_path = mock_post.call_args_list[0].args[1]
        self.assertEqual(admin_path, "dicts/admin/country-codes")

    @patch("seeder._post_one")
    @patch("seeder._get_existing")
    def test_idempotent_when_all_exist(self, mock_existing, mock_post):
        mock_existing.return_value = {"POL", "DEU", "USA"}
        added = seeder.seed_country_codes("http://api/api", "tok", ["POL", "DEU", "USA"])
        self.assertEqual(added, 0)
        mock_post.assert_not_called()

    @patch("seeder._post_one")
    @patch("seeder._get_existing")
    def test_409_does_not_count_as_added(self, mock_existing, mock_post):
        mock_existing.return_value = set()
        mock_post.return_value = False  # 409
        added = seeder.seed_country_codes("http://api/api", "tok", ["POL", "DEU"])
        self.assertEqual(added, 0)
        self.assertEqual(mock_post.call_count, 2)


class TestGetAccessToken(unittest.TestCase):
    @patch.dict(
        os.environ,
        {
            "SCRAPPER_KEYCLOAK_BASE_URL": "http://kc",
            "SCRAPPER_KEYCLOAK_REALM": "coolture-dev",
            "SCRAPPER_KEYCLOAK_CLIENT_ID": "coolture-bot",
            "SCRAPPER_KEYCLOAK_CLIENT_SECRET": "topsecret",
        },
        clear=False,
    )
    @patch("seeder.requests.post")
    def test_uses_client_credentials_grant(self, mock_post):
        mock_post.return_value = _resp(200, json_data={"access_token": "tok"})
        token = seeder._get_access_token()
        self.assertEqual(token, "tok")

        _, kwargs = mock_post.call_args
        self.assertEqual(kwargs["data"]["grant_type"], "client_credentials")
        self.assertEqual(kwargs["data"]["client_id"], "coolture-bot")
        self.assertEqual(kwargs["data"]["client_secret"], "topsecret")

    @patch.dict(os.environ, {"SCRAPPER_KEYCLOAK_CLIENT_SECRET": ""}, clear=False)
    def test_raises_without_secret(self):
        with self.assertRaises(RuntimeError):
            seeder._get_access_token()


class TestSeedOrchestration(unittest.TestCase):
    """End-to-end test of seed() with everything below it mocked."""

    @patch("seeder._wait_for_dependencies")
    @patch("seeder._get_access_token", return_value="tok")
    @patch("seeder.seed_country_codes", return_value=5)
    def test_returns_counts(self, mock_codes, mock_token, mock_wait):
        result = seeder.seed()
        self.assertEqual(result, {"codes_added": 5})
        mock_wait.assert_called_once()
        mock_token.assert_called_once()

    @patch("seeder._wait_for_dependencies")
    @patch("seeder._get_access_token", return_value="tok")
    @patch("seeder.seed_country_codes", return_value=0)
    def test_zero_when_already_seeded(self, mock_codes, mock_token, mock_wait):
        result = seeder.seed()
        self.assertEqual(result, {"codes_added": 0})


class TestDefaults(unittest.TestCase):
    """Smoke checks on the default seed list."""

    def test_pol_is_in_default_country_codes(self):
        self.assertIn("POL", seeder.DEFAULT_COUNTRY_CODES)

    def test_country_codes_are_iso3_uppercase(self):
        for code in seeder.DEFAULT_COUNTRY_CODES:
            self.assertEqual(len(code), 3, code)
            self.assertTrue(code.isupper() and code.isalpha(), code)

    def test_no_duplicates_in_defaults(self):
        self.assertEqual(len(seeder.DEFAULT_COUNTRY_CODES), len(set(seeder.DEFAULT_COUNTRY_CODES)))


if __name__ == "__main__":
    unittest.main()
