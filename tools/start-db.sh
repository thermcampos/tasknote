#!/bin/bash

export $(cat .env | xargs)

docker run -d --rm \
  --name db \
  --network=host \
  -e POSTGRES_DB=$POSTGRES_DB \
  -e POSTGRES_USER=$POSTGRES_USER \
  -e POSTGRES_PASSWORD=$POSTGRES_PASSWORD \
  -e PGDATA=$PGDATA \
  -v ./data:/tmp \
  -v /backups:/backups \
  postgres:15.8-bookworm
