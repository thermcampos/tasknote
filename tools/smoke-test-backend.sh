#!/usr/bin/env bash
# Smoke test for a built server image.
# Starts Postgres + the given server image, then exercises:
#   sign-up -> confirm email -> sign-in -> create task -> list tasks
#
# Usage: SERVER_IMAGE=ghcr.io/thermcampos/tasknote/server:pr-123 bash tools/smoke-test-backend.sh
# Optional: GHCR_USER + GHCR_TOKEN to log in to GHCR before pulling.
set -euo pipefail

SERVER_IMAGE="${SERVER_IMAGE:?SERVER_IMAGE env var is required}"
DB_IMAGE="postgres:15.8-bookworm"
NETWORK="tasknote-smoke"
DB_CONTAINER="tasknote-smoke-db"
API_CONTAINER="tasknote-smoke-api"
DB_PASSWORD="default"
API_URL="http://localhost:8585"
EMAIL="smoke-test@tasknote.cc"
PASSWORD="Teste@123"
TASK_DESCRIPTION="smoke test task $(date +%s)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cleanup() {
  local status=$?
  if [ "$status" -ne 0 ]; then
    echo "Smoke test failed. Server logs:"
    docker logs "$API_CONTAINER" 2>&1 | tail -n 200 || true
  fi
  docker rm -f "$API_CONTAINER" "$DB_CONTAINER" >/dev/null 2>&1 || true
  docker network rm "$NETWORK" >/dev/null 2>&1 || true
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

echo "Pulling images..."
docker pull "$SERVER_IMAGE"
docker pull "$DB_IMAGE"

docker network create "$NETWORK" >/dev/null

echo "Starting Postgres..."
docker run -d --name "$DB_CONTAINER" --network "$NETWORK" -p 5432:5432 \
  -e POSTGRES_DB=tasknote \
  -e POSTGRES_USER=tasknoteuser \
  -e POSTGRES_PASSWORD="$DB_PASSWORD" \
  "$DB_IMAGE" >/dev/null

for _ in $(seq 1 30); do
  if docker exec "$DB_CONTAINER" pg_isready -U tasknoteuser -d tasknote >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
docker exec "$DB_CONTAINER" pg_isready -U tasknoteuser -d tasknote >/dev/null || fail "Postgres not ready"

echo "Starting server ${SERVER_IMAGE}..."
docker run -d --name "$API_CONTAINER" --network "$NETWORK" -p 8585:8585 \
  -e POSTGRES_DB=tasknote \
  -e POSTGRES_HOST="$DB_CONTAINER" \
  -e POSTGRES_USER=tasknoteuser \
  -e POSTGRES_PASSWORD="$DB_PASSWORD" \
  -e CORS_ALLOWED_ORIGINS=http://localhost:5000 \
  -e TARGET_ENV=development \
  -e SECURITY_KEY=this-is-a-very-long-security-key-for-smoke-tests \
  -e RESEND_APIKEY=invalid-api-key-only-placeholder \
  "$SERVER_IMAGE" >/dev/null

echo "Waiting for ${API_URL}/health..."
healthy=false
for _ in $(seq 1 60); do
  if curl -fsS "${API_URL}/health" >/dev/null 2>&1; then
    healthy=true
    break
  fi
  sleep 2
done
[ "$healthy" = true ] || fail "server did not become healthy"
echo "OK: health"

echo "Signing up ${EMAIL}..."
status=$(curl -s -o /dev/null -w '%{http_code}' -X PUT \
  -H 'Content-Type: application/json' \
  "${API_URL}/auth/sign-up" \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\",\"passwordAgain\":\"${PASSWORD}\",\"lang\":\"en\"}")
[ "$status" = "204" ] || fail "sign-up returned HTTP ${status}, expected 204"
echo "OK: sign-up"

echo "Confirming email..."
PGPASSWORD="$DB_PASSWORD" bash "${SCRIPT_DIR}/confirm-user.sh"
echo "OK: email confirmed"

echo "Signing in..."
body=$(curl -s -w '\n%{http_code}' -X POST \
  -H 'Content-Type: application/json' \
  "${API_URL}/auth/sign-in" \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\"}")
status=$(tail -n1 <<<"$body")
[ "$status" = "200" ] || fail "sign-in returned HTTP ${status}, expected 200: $(head -n -1 <<<"$body")"
TOKEN=$(head -n -1 <<<"$body" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
[ -n "$TOKEN" ] || fail "sign-in response has no token"
echo "OK: sign-in"

echo "Creating task..."
DUE_DATE=$(date -d '+7 days' +%F)
status=$(curl -s -o /dev/null -w '%{http_code}' -X POST \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer ${TOKEN}" \
  "${API_URL}/rest/tasks" \
  -d "{\"description\":\"${TASK_DESCRIPTION}\",\"urls\":[\"https://tasknote.cc\"],\"dueDate\":\"${DUE_DATE}\",\"highPriority\":true,\"tags\":[\"smoke\"]}")
[ "$status" = "201" ] || fail "create task returned HTTP ${status}, expected 201"
echo "OK: create task"

echo "Fetching tasks..."
body=$(curl -s -w '\n%{http_code}' \
  -H "Authorization: Bearer ${TOKEN}" \
  "${API_URL}/rest/tasks")
status=$(tail -n1 <<<"$body")
[ "$status" = "200" ] || fail "list tasks returned HTTP ${status}, expected 200"
grep -q "$TASK_DESCRIPTION" <<<"$body" || fail "created task not found in task list"
echo "OK: list tasks"

echo "Backend smoke test passed."
