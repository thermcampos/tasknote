#!/bin/bash
# Client - Front-end E2E (Cypress, headless)
#
# Specs stub API calls with cy.intercept, so only the Vite dev server is needed
# (no API/DB). If something already answers on the Cypress baseUrl port (e.g. the
# docker compose dev stack), it is reused; otherwise Vite is started and stopped here.

set -u

CURRENT_DIR=$(pwd)
if [ ! -f "$CURRENT_DIR/package.json" ]; then
  cd "$CURRENT_DIR/client" || exit 1
fi

PORT=5000
BASE_URL="http://localhost:$PORT"
VITE_PID=""

cleanup() {
  if [ -n "$VITE_PID" ]; then
    kill "$VITE_PID" 2>/dev/null
    wait "$VITE_PID" 2>/dev/null
  fi
}
trap cleanup EXIT

if curl -s -o /dev/null "$BASE_URL"; then
  echo "Reusing app already running on $BASE_URL"
else
  echo "Starting Vite on $BASE_URL..."
  ./node_modules/.bin/vite --port "$PORT" --strictPort > vite-e2e.log 2>&1 &
  VITE_PID=$!
  for _ in $(seq 60); do
    curl -s -o /dev/null "$BASE_URL" && break
    if ! kill -0 "$VITE_PID" 2>/dev/null; then
      echo "Vite exited early:"; cat vite-e2e.log; exit 1
    fi
    sleep 1
  done
  if ! curl -s -o /dev/null "$BASE_URL"; then
    echo "Vite did not start within 60s:"; cat vite-e2e.log; exit 1
  fi
fi

echo "Running Cypress headless..."
npm run cypress:run:headless
STATUS=$?

if [ $STATUS -ne 0 ]; then
  echo "Cypress E2E failed. Screenshots: client/cypress/screenshots"
  exit $STATUS
fi

echo "E2E passed!"
exit 0
