#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ROOT_DIR="$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)"
SQL_FILE="$SCRIPT_DIR/init-db.sql"

if [ -f "$ROOT_DIR/.env" ]; then
	# shellcheck disable=SC1091
	set -a
	. "$ROOT_DIR/.env"
	set +a
fi

: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"

echo "Applying $SQL_FILE to database '$POSTGRES_DB'..."
docker compose -f "$ROOT_DIR/docker-compose.yml" exec -T db \
	psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" < "$SQL_FILE"
echo "Done."
