#!/bin/bash
# Exercises every task endpoint (used to collect GraalVM reachability metadata).
# Requires $TOKEN (see sign-up-and-login.sh). Optional: $API_URL.

API_URL="${API_URL:-http://localhost:8585}"

if [ -z "$TOKEN" ]; then
  echo "TOKEN is not set. Sign in first (see sign-up-and-login.sh)."
  exit 1
fi

api() {
  local method=$1 path=$2 body=$3
  if [ -n "$body" ]; then
    curl -X "$method" -s \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      "$API_URL$path" -d "$body"
  else
    curl -X "$method" -s \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      "$API_URL$path"
  fi
}

section() {
  echo ""
  echo "=== $1"
}

section "Creating task (POST /rest/tasks)"
CREATED=$(api POST /rest/tasks '{
    "description": "task from shell script testing api",
    "urls": ["https://gentoo.org"],
    "dueDate": "2026-10-11",
    "highPriority": true,
    "tags": ["bash", "test"]
  }')
echo "$CREATED" | jq
ID=$(echo "$CREATED" | jq -r '.id')
if [ -z "$ID" ] || [ "$ID" = "null" ]; then
  echo "Failed to create task, aborting."
  exit 1
fi
echo "Created task with id: $ID"

section "Getting task by id (GET /rest/tasks/$ID)"
api GET "/rest/tasks/$ID" | jq

section "Getting all tasks (GET /rest/tasks)"
api GET /rest/tasks | jq

section "Updating task (PATCH /rest/tasks/$ID)"
api PATCH "/rest/tasks/$ID" '{
    "completed": true,
    "description": "task updated from shell script testing api",
    "urls": ["https://kernel.org", "https://gentoo.org"],
    "dueDate": "2026-12-31",
    "highPriority": false,
    "tags": ["bash", "updated"]
  }' | jq

section "Deleting task (DELETE /rest/tasks/$ID)"
curl -X DELETE -s -o /dev/null -w "HTTP %{http_code}\n" \
  -H "Authorization: Bearer $TOKEN" \
  "$API_URL/rest/tasks/$ID"

section "Getting deleted task, expecting an error (GET /rest/tasks/$ID)"
api GET "/rest/tasks/$ID" | jq
