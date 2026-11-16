#!/bin/bash
# Exercises every note endpoint (used to collect GraalVM reachability metadata).
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

section "Creating note (POST /rest/notes)"
CREATED=$(api POST /rest/notes '{
    "title": "Note created from bash script",
    "description": "note content from shell script testing api",
    "url": "https://gentoo.org",
    "tags": ["bash", "test"]
  }')
echo "$CREATED" | jq
ID=$(echo "$CREATED" | jq -r '.id')
if [ -z "$ID" ] || [ "$ID" = "null" ]; then
  echo "Failed to create note, aborting."
  exit 1
fi
echo "Created note with id: $ID"

section "Getting note by id (GET /rest/notes/$ID)"
api GET "/rest/notes/$ID" | jq

section "Getting all notes (GET /rest/notes)"
api GET /rest/notes | jq

section "Updating note (PATCH /rest/notes/$ID)"
api PATCH "/rest/notes/$ID" '{
    "title": "Note updated from bash script",
    "description": "updated note content from shell script testing api",
    "url": "https://kernel.org",
    "tags": ["bash", "updated"]
  }' | jq

section "Sharing note (PUT /rest/notes/$ID/share)"
SHARED=$(api PUT "/rest/notes/$ID/share")
echo "$SHARED" | jq
SHARE_TOKEN=$(echo "$SHARED" | jq -r '.shareToken')

section "Getting shared note (GET /public/notes/$SHARE_TOKEN)"
curl -X GET -s \
  -H "Content-Type: application/json" \
  "$API_URL/public/notes/$SHARE_TOKEN" | jq

section "Unsharing note (PUT /rest/notes/$ID/unshare)"
api PUT "/rest/notes/$ID/unshare" | jq

section "Archiving note (PUT /rest/notes/$ID/archive)"
api PUT "/rest/notes/$ID/archive" | jq

section "Restoring note (PUT /rest/notes/$ID/restore)"
api PUT "/rest/notes/$ID/restore" | jq

section "Deleting non-archived note, expecting an error (DELETE /rest/notes/$ID)"
api DELETE "/rest/notes/$ID" | jq

section "Archiving note again (PUT /rest/notes/$ID/archive)"
api PUT "/rest/notes/$ID/archive" | jq

section "Deleting archived note (DELETE /rest/notes/$ID)"
curl -X DELETE -s -o /dev/null -w "HTTP %{http_code}\n" \
  -H "Authorization: Bearer $TOKEN" \
  "$API_URL/rest/notes/$ID"

section "Getting deleted note, expecting an error (GET /rest/notes/$ID)"
api GET "/rest/notes/$ID" | jq
