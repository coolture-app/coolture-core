#!/bin/sh

set -eu

G="chroot /proc/1/root /garage"

for i in $(seq 1 20); do
  if $G node id -q 2>/dev/null; then break; fi
  echo "Waiting for Garage RPC... ($i/20)"
  sleep 3
done
$G node id -q || { echo "Garage RPC not ready"; exit 1; }

if $G status 2>&1 | grep -q 'NO ROLE ASSIGNED'; then
  NODE_ID=$($G node id -q | cut -c1-16)
  $G layout assign "$NODE_ID" -z local -c 1G
  $G layout apply --version 1
fi

$G key import --yes \
  -n "$GARAGE_KEY_NAME" \
  "$GARAGE_ACCESS_KEY_ID" \
  "$GARAGE_ACCESS_KEY_SECRET" || true
$G bucket create "$GARAGE_BUCKET_NAME" || true
$G bucket allow "$GARAGE_BUCKET_NAME" \
  --key "$GARAGE_KEY_NAME" \
  --read --write --owner || true

echo "Garage initialized."