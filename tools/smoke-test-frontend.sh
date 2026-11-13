#!/usr/bin/env bash
# Smoke test for a built client image.
# Starts the given client image and checks that Caddy serves the app shell,
# the runtime env.js, and falls back to index.html for client-side routes.
#
# Usage: APP_IMAGE=ghcr.io/thermcampos/tasknote/client:pr-123 bash tools/smoke-test-frontend.sh
# Optional: GHCR_USER + GHCR_TOKEN to log in to GHCR before pulling.
set -euo pipefail

APP_IMAGE="${APP_IMAGE:?APP_IMAGE env var is required}"
APP_CONTAINER="tasknote-smoke-app"
APP_URL="http://localhost:5000"
BACKEND_URL="http://localhost:8585"

cleanup() {
  local status=$?
  if [ "$status" -ne 0 ]; then
    echo "Smoke test failed. App logs:"
    docker logs "$APP_CONTAINER" 2>&1 | tail -n 200 || true
  fi
  docker rm -f "$APP_CONTAINER" >/dev/null 2>&1 || true
  exit "$status"
}
trap cleanup EXIT

fail() {
  echo "FAIL: $*"
  exit 1
}

if [ -n "${GHCR_TOKEN:-}" ]; then
  echo "$GHCR_TOKEN" | docker login ghcr.io -u "${GHCR_USER:?GHCR_USER is required with GHCR_TOKEN}" --password-stdin
fi

echo "Pulling ${APP_IMAGE}..."
docker pull "$APP_IMAGE"

echo "Starting app..."
docker run -d --name "$APP_CONTAINER" -p 5000:5000 \
  -e VITE_BACKEND_SERVER="$BACKEND_URL" \
  "$APP_IMAGE" >/dev/null

echo "Waiting for ${APP_URL}..."
ready=false
for _ in $(seq 1 30); do
  if curl -fsS "$APP_URL" >/dev/null 2>&1; then
    ready=true
    break
  fi
  sleep 1
done
[ "$ready" = true ] || fail "app did not respond"

echo "Fetching index..."
body=$(curl -fsS "$APP_URL/")
grep -q '<div id="root"></div>' <<<"$body" || fail "index.html has no #root element"
echo "OK: index"

echo "Fetching env.js..."
body=$(curl -fsS "$APP_URL/env.js")
grep -q "\"VITE_BACKEND_SERVER\":\"${BACKEND_URL}\"" <<<"$body" || fail "env.js missing VITE_BACKEND_SERVER: ${body}"
echo "OK: env.js"

echo "Fetching a client-side route..."
body=$(curl -fsS "$APP_URL/login")
grep -q '<div id="root"></div>' <<<"$body" || fail "SPA fallback did not serve index.html"
echo "OK: SPA fallback"

echo "Frontend smoke test passed."
