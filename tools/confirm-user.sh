#!/usr/bin/env bash
set -euo pipefail

docker run --rm -i --network=host \
  -e PGPASSWORD="${PGPASSWORD:?PGPASSWORD env var is required}" \
  postgres:15.8-bookworm \
  psql -h localhost -U tasknoteuser -d tasknote \
  -c "UPDATE tasknote.users SET email_confirmed_at = created_at WHERE id > 0;"
