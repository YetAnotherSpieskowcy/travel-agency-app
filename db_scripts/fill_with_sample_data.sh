#!/bin/bash

set -xeo pipefail

MONGO_HOST="${MONGO_HOST:-student-swarm01.maas}"
MONGO_PORT="${MONGO_PORT:-27017}"
MONGO_URL="mongodb://$MONGO_USERNAME:$MONGO_PASSWORD@$MONGO_HOST:$MONGO_PORT/"

POSTGRES_HOST="${POSTGRES_HOST:-student-swarm01.maas}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_DB="${POSTGRES_DB:-5432}"

db_scripts=$(realpath db_scripts)

# MongoDB - document schemas
docker run -it --rm \
--network host \
-v "$db_scripts/mongo:/scripts" \
-e "MONGO_URL=$MONGO_URL" \
mongo:4.4.9 \
bash -O nullglob -c 'for file in /scripts/*.js; do mongo "$MONGO_URL" "$file"; done'

# MongoDB - samples
docker run -it --rm \
--network host \
-v "$db_scripts/mongo/50_samples:/scripts" \
-e "MONGO_URL=$MONGO_URL" \
mongo:4.4.9 \
bash -O nullglob -c 'for file in /scripts/*.js; do mongo "$MONGO_URL" "$file"; done'


# PostgreSQL - create database
docker run -it --rm \
--network host \
-v "$db_scripts/sql:/scripts" \
-e "PGUSER=$POSTGRES_USERNAME" \
-e "PGPASSWORD=$POSTGRES_PASSWORD" \
-e "PGHOST=$POSTGRES_HOST" \
-e "PGPORT=$POSTGRES_PORT" \
postgres:13 \
psql -f /scripts/00_create_db.sql

# PostgreSQL - table schemas
docker run -it --rm \
--network host \
-v "$db_scripts/sql:/scripts" \
-e "PGUSER=$POSTGRES_USERNAME" \
-e "PGPASSWORD=$POSTGRES_PASSWORD" \
-e "PGHOST=$POSTGRES_HOST" \
-e "PGPORT=$POSTGRES_PORT" \
-e "PGDATABASE=$POSTGRES_DB" \
postgres:13 \
bash -O nullglob -c 'for file in /scripts/*.sql; do psql -f "$file"; done'

# PostgreSQL - samples
docker run -it --rm \
--network host \
-v "$db_scripts/sql/50_samples:/scripts" \
-e "PGUSER=$POSTGRES_USERNAME" \
-e "PGPASSWORD=$POSTGRES_PASSWORD" \
-e "PGHOST=$POSTGRES_HOST" \
-e "PGPORT=$POSTGRES_PORT" \
-e "PGDATABASE=$POSTGRES_DB" \
postgres:13 \
bash -O nullglob -c 'for file in /scripts/*.sql; do psql -f "$file"; done'
