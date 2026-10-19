#!/usr/bin/env bash
# Restore SAM from a backup directory produced by backup.sh.
# DESTRUCTIVE: replaces the contents of the databases in the backup and the sheet music files.
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
"${COMPOSE[@]}" up -d "${dbs[@]}"

restore() { # <service> <dump file>
  until "${COMPOSE[@]}" exec -T "$1" pg_isready -q; do sleep 1; done
  "${COMPOSE[@]}" exec -T "$1" sh -c \
    'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner --exit-on-error' <"$2"
}
echo "Restoring databases"
restore database "$SRC/sam_music.dump"
[ -f "$SRC/keycloak.dump" ] && restore keycloak-db "$SRC/keycloak.dump"

echo "Restoring sheet music files"
"${COMPOSE[@]}" run --rm -T --no-deps restore-files sh -c \
  "find /data -mindepth 1 -delete && tar xzf /in/$name/sam-data.tar.gz -C /data"

echo "Starting the stack"
"${COMPOSE[@]}" up -d
