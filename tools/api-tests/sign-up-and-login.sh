#!/bin/bash

# Create user
curl -X PUT \
  -H "Content-Type:application/json" \
  http://localhost:8585/auth/sign-up \
  -d '{"email":"email@email.com","password":"Teste@123","passwordAgain":"Teste@123","lang":"en"}'

# Confirm email
PGPASSWORD=default ./tools/confirm-user.sh

# Login user
curl -X POST \
  -H "Content-Type:application/json" \
  http://localhost:8585/auth/sign-in \
  -d '{"email":"email@email.com","password":"Teste@123"}'
