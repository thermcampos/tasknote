#!/bin/bash

# Fetch home items
echo "Fetching home items..."
curl -X GET -s \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8585/rest/home/items?q=vps | jq

