#!/bin/sh
set -e

case "${SCRAPPER_INGEST_ENABLED:-true}" in
  true|TRUE|True)
    echo "[entrypoint] running data seeder..."
    python -u seeder.py
    ;;
  *)
    echo "[entrypoint] skipping data seeder because SCRAPPER_INGEST_ENABLED=${SCRAPPER_INGEST_ENABLED:-true}"
    ;;
esac

echo "[entrypoint] starting scrapper..."
exec python -u 3city.py
