#!/bin/bash

# Create note
echo "Creating note..."
curl -X POST -s \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8585/rest/notes \
  -d '{
    "title": "Note created from bash script",
    "description": "note content from shell script testing api",
    "url": "https://gentoo.org",
    "tags": ["bash", "test"]
  }' | jq

echo ""
echo ""
echo ""
echo "Getting notes.."
# Get notes
curl -X GET -s \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  http://localhost:8585/rest/notes | jq

echo ""
echo ""
echo ""

# Create note
#curl -X POST \
#  -H "Content-Type:application/json" \
#  http://localhost:8585/auth/sign-in \
 # -d '{"email":"email@email.com","password":"Teste@123","passwordAgain":"Teste@123","lang":"en"}'
