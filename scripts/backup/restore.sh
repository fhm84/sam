#!/usr/bin/env bash
# Restore SAM from a backup directory produced by backup.sh.
# DESTRUCTIVE: replaces the contents of both databases and the sheet music files.
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

echo "Stopping application services"
"${COMPOSE[@]}" stop sam-ui sam-server keycloak
"${COMPOSE[@]}" up -d database keycloak-db

restore() { # <service> <dump file>
  until "${COMPOSE[@]}" exec -T "$1" pg_isready -q; do sleep 1; done
  "${COMPOSE[@]}" exec -T "$1" sh -c \
    'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner --exit-on-error' <"$2"
}
echo "Restoring databases"
restore database "$SRC/sam_music.dump"
restore keycloak-db "$SRC/keycloak.dump"

echo "Restoring sheet music files"
"${COMPOSE[@]}" run --rm -T --no-deps restore-files sh -c \
  "find /data -mindepth 1 -delete && tar xzf /in/$name/sam-data.tar.gz -C /data"

echo "Starting the stack"
"${COMPOSE[@]}" up -d
