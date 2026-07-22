#!/bin/bash

echo "Getting env vars..."
export $(cat .env | xargs)

docker exec db pg_dump \
  -U ${POSTGRES_USER} \
  -h ${POSTGRES_HOST} \
  -d ${POSTGRES_DB} \
  --data-only \
  --schema=tasknote \
  --inserts \
  --no-comments \
  --on-conflict-do-nothing \
  --file=/backups/backup_$(date +%Y-%m-%d).sql
  
#restore:
#psql -h ${TARGET_HOST} -U ${POSTGRESQL_USER} -d ${POSTGRESQL_DATABASE} -f $sql_file
