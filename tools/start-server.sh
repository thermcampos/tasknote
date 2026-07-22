#!/bin/bash
#

PR="$1"
echo "PR: $PR"

echo "Getting env vars..."
export $(cat .env | xargs)

docker run -d  --rm \
  --name server \
  --network=host \
  -e POSTGRES_DB=$POSTGRES_DB \
  -e POSTGRES_USER=$POSTGRES_USER \
  -e POSTGRES_PASSWORD=$POSTGRES_PASSWORD \
  -e POSTGRES_PORT=$POSTGRES_PORT \
  -e POSTGRES_HOST=$POSTGRES_HOST \
  -e CORS_ALLOWED_ORIGINS=$CORS_ALLOWED_ORIGINS \
  -e SERVER_SERVLET_CONTEXT_PATH=$SERVER_SERVLET_CONTEXT_PATH \
  -e MAILGUN_APIKEY=$MAILGUN_APIKEY \
  -e SECURITY_KEY=$SECURITY_KEY \
  -e BUILD=ghcr.io/ricardo-campos-org/react-typescript-todolist/server:$PR \
  ghcr.io/ricardo-campos-org/react-typescript-todolist/server:$PR
