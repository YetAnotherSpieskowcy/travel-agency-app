#!/bin/bash

set -xeo pipefail

MONGO_PORT="${MONGO_PORT:-27017}"
MONGO_URL="mongodb://$MONGO_USERNAME:$MONGO_PASSWORD@$MONGO_HOST:$MONGO_PORT/"

export PGHOST="${POSTGRES_HOST:-student-swarm01.maas}"
export PGPORT="${POSTGRES_PORT:-5432}"
export PGDATABASE="${POSTGRES_DB:-5432}"
export PGUSER="$POSTGRES_USERNAME"
export PGPASSWORD="$POSTGRES_PASSWORD"

# Wait for DB servers to be healthy
db_ready=0
for _ in {0..5}; do
    sleep 1
	if ! mongo --quiet "$MONGO_URL" --eval 'quit(db.runCommand({ ping: 1 }).ok ? 0 : 2)'; then
		continue
	fi
	if ! pg_isready; then
	    continue
	fi
	db_ready=1
	break
done
if [[ "$db_ready" -eq 0 ]]; then
	echo 'Failed to connect to DBs'
	exit 1
fi

# MongoDB
find /scripts/mongo -name '*.js' -print0 | xargs -0 -n 1 mongo "$MONGO_URL"
echo 'MongoDB: Sample data filling finished.'

# PostgreSQL - create database using maintenance db
PGDATABASE='' psql -v 'ON_ERROR_STOP=1' -f /scripts/sql/00_create_db.sql
# PostgreSQL - run all SQL scripts
find /scripts/sql -name '*.sql' -print0 | xargs -0 -n 1 psql -v 'ON_ERROR_STOP=1' -f >/dev/null
echo 'PostgreSQL: Sample data filling finished.'

echo '*: Sample data filling finished.'
