#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ROOT_DIR="$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)"
SQL_FILE="$SCRIPT_DIR/seed-users.sql"

if [ -f "$ROOT_DIR/.env" ]; then
	# shellcheck disable=SC1091
	set -a
	. "$ROOT_DIR/.env"
	set +a
fi

: "${POSTGRES_HOST:?POSTGRES_HOST is required}"
: "${POSTGRES_PORT:?POSTGRES_PORT is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"

echo "Seeding test users into database '$POSTGRES_DB'..."
PGPASSWORD="$POSTGRES_PASSWORD" psql \
	-v ON_ERROR_STOP=1 \
	-h "$POSTGRES_HOST" \
	-p "$POSTGRES_PORT" \
	-U "$POSTGRES_USER" \
	-d "$POSTGRES_DB" \
	-f "$SQL_FILE"
echo "Done."
