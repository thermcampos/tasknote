#!/bin/bash

# Create task
echo "Creating task..."
curl -X POST -s \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8585/rest/tasks \
  -d '{
    "description": "task from shell script testing api",
    "urls": ["https://gentoo.org"],
    "dueDate": "2026-10-11",
    "highPriority": true,
    "tags": ["bash", "test"]
  }' | jq

echo ""
echo ""
echo ""
echo "Getting tasks.."
# Get tasks
curl -X GET -s \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8585/rest/tasks | jq

echo ""
echo ""
echo ""

# Create note
#curl -X POST \
#  -H "Content-Type:application/json" \
#  http://localhost:8585/auth/sign-in \
 # -d '{"email":"email@email.com","password":"Teste@123","passwordAgain":"Teste@123","lang":"en"}'
