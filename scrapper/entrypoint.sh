#!/bin/sh
set -e

echo "[entrypoint] running data seeder..."
python -u seeder.py

echo "[entrypoint] starting scrapper..."
exec python -u 3city.py
