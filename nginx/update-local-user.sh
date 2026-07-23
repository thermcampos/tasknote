#!/bin/bash

CONTAINER="tasknote-db"
SQL_USER="tasknoteuser"
SQL_DATABASE="tasknote"
SQL_SCHEMA="tasknote"
SQL_TABLE_NAME="users"
SQL_QUERY="UPDATE $SQL_SCHEMA.$SQL_TABLE_NAME SET email_confirmed_at = created_at WHERE id = 1;"

docker exec -it $CONTAINER \
  psql \
  -U $SQL_USER \
  -d $SQL_DATABASE \
  -c "$SQL_QUERY"

