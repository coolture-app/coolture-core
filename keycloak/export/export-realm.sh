#!/bin/bash
set -e

# fix paths for Windows
export MSYS_NO_PATHCONV=1

# Go to project root
cd "$(dirname "$0")/../../" 
PROJECT_ROOT=$(pwd)

echo "Working from project root: $PROJECT_ROOT"

echo "Stopping Keycloak container..."
docker-compose -f docker-compose.base.yml stop keycloak

# Create export directory if it doesn't exist
mkdir -p "${PROJECT_ROOT}/keycloak/export"

# Export realm without users
echo "Exporting realm 'coolture-dev' (users excluded)..."
docker-compose -f docker-compose.base.yml run --rm \
  -v "${PROJECT_ROOT}/keycloak/export:/tmp/export" \
  keycloak \
  export \
    --file /tmp/export/realm-export.json \
    --realm coolture-dev

echo "Export completed successfully. Starting Keycloak again."
# Sleep to avoid port conflicts before clearout of previous export container
sleep 2
docker-compose -f docker-compose.base.yml start keycloak
echo "Files written to: ${PROJECT_ROOT}/keycloak/export/"