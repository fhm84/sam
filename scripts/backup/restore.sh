#!/usr/bin/env bash
# Restore SAM from a backup directory produced by backup.sh.
# DESTRUCTIVE: replaces the contents of the databases in the backup, Pocket ID's data (if the
# backup has it) and the sheet music files.
#
#   scripts/backup/restore.sh /var/backups/sam/2026-09-19_030000 --yes
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SRC="${1:?usage: restore.sh <backup-dir> --yes}"
[ "${2:-}" = "--yes" ] || { echo "Refusing to overwrite data without --yes" >&2; exit 1; }
export BACKUP_DIR="$(cd "$SRC/.." && pwd)"
name="$(basename "$SRC")"
ENV_FILE="${ENV_FILE:-$ROOT/.env}"
COMPOSE=(docker compose -f "$ROOT/docker-compose.prod.yml" --env-file "$ENV_FILE")

(cd "$SRC" && sha256sum -c SHA256SUMS)

# Services of the active provider (COMPOSE_PROFILES in .env)
services="$("${COMPOSE[@]}" config --services)"
has() { grep -qx "$1" <<<"$services"; }

echo "Stopping application services"
app=(sam-ui sam-server)
for s in keycloak pocket-id; do has "$s" && app+=("$s"); done
"${COMPOSE[@]}" stop "${app[@]}"
dbs=(database)
if [ -f "$SRC/keycloak.dump" ]; then
  has keycloak-db || { echo "The backup has a Keycloak database, but the keycloak profile is not active" >&2; exit 1; }
  dbs+=(keycloak-db)
fi
if [ -f "$SRC/pocket-id.zip" ]; then
  has pocket-id || { echo "The backup has Pocket ID data, but the pocket-id profile is not active" >&2; exit 1; }
elif has pocket-id; then
  echo "NOTE: the backup has no Pocket ID data (older than #216); Pocket ID keeps its current data" >&2
fi
"${COMPOSE[@]}" up -d "${dbs[@]}"

restore() { # <service> <dump file>
  until "${COMPOSE[@]}" exec -T "$1" pg_isready -q; do sleep 1; done
  "${COMPOSE[@]}" exec -T "$1" sh -c \
    'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner --exit-on-error' <"$2"
}
echo "Restoring databases"
restore database "$SRC/sam_music.dump"
[ -f "$SRC/keycloak.dump" ] && restore keycloak-db "$SRC/keycloak.dump"

if [ -f "$SRC/pocket-id.zip" ]; then
  # Needs the stopped service: import takes exclusive access to Pocket ID's database
  echo "Restoring Pocket ID"
  "${COMPOSE[@]}" run --rm -T --no-deps pocket-id /app/pocket-id import -p - -y <"$SRC/pocket-id.zip"
fi

echo "Restoring sheet music files"
"${COMPOSE[@]}" run --rm -T --no-deps restore-files sh -c \
  "find /data -mindepth 1 -delete && tar xzf /in/$name/sam-data.tar.gz -C /data"

echo "Starting the stack"
"${COMPOSE[@]}" up -d
