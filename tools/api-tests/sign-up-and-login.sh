#!/bin/bash
# Signs up, confirms and signs in the test user, exporting TOKEN.
# Run from the repo root. Source it (`. tools/api-tests/sign-up-and-login.sh`)
# to keep TOKEN in the calling shell.

# Create user
curl -X PUT \
  -H "Content-Type:application/json" \
  http://localhost:8585/auth/sign-up \
  -d '{"email":"email@email.com","password":"Teste@123","passwordAgain":"Teste@123","lang":"en"}'
echo ""

# Confirm email
PGPASSWORD=default ./tools/confirm-user.sh

# Login user
SIGN_IN_RESPONSE=$(curl -X POST -s \
  -H "Content-Type:application/json" \
  http://localhost:8585/auth/sign-in \
  -d '{"email":"email@email.com","password":"Teste@123"}')
echo "$SIGN_IN_RESPONSE"

TOKEN=$(echo "$SIGN_IN_RESPONSE" | jq -r '.token // empty')
export TOKEN
