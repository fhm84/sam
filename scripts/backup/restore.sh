#!/usr/bin/env bash
# Restore SAM from a backup directory produced by backup.sh.
# DESTRUCTIVE: replaces the contents of the databases in the backup, Pocket ID's data (if the
# backup has it) and the sheet music files.
#
#   scripts/backup/restore.sh /var/backups/sam/2026-09-19_030000 --yes
#
# An encrypted backup (*.age files, see backup.sh) needs the private key and the `age` command:
#
#   BACKUP_AGE_IDENTITY=/path/to/key.txt scripts/backup/restore.sh <backup-dir> --yes
#
# .env is not restored: on a new machine, get it back first with
#   age -d -i /path/to/key.txt <backup-dir>/env.age > .env
#
# RESTORE_START=no leaves the stack stopped at the end (the databases keep running), for
# starting it yourself, for example with further compose files.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SRC="${1:?usage: restore.sh <backup-dir> --yes}"
[ "${2:-}" = "--yes" ] || { echo "Refusing to overwrite data without --yes" >&2; exit 1; }
BACKUP_DIR="$(cd "$SRC/.." && pwd)"
export BACKUP_DIR
ENV_FILE="${ENV_FILE:-$ROOT/.env}"
COMPOSE=(docker compose -f "$ROOT/docker-compose.prod.yml" --env-file "$ENV_FILE")

(cd "$SRC" && sha256sum -c SHA256SUMS)

# A file of the backup, plain or encrypted
has_file() { [ -f "$SRC/$1" ] || [ -f "$SRC/$1.age" ]; }
content() { # <name>: the file's plain content on stdout
  if [ -f "$SRC/$1.age" ]; then
    age -d -i "$BACKUP_AGE_IDENTITY" "$SRC/$1.age"
  else
    cat "$SRC/$1"
  fi
}
if compgen -G "$SRC/*.age" >/dev/null; then
  command -v age >/dev/null || { echo "The backup is encrypted, but the age command is missing" >&2; exit 1; }
  [ -r "${BACKUP_AGE_IDENTITY:-}" ] || { echo "The backup is encrypted: set BACKUP_AGE_IDENTITY to the private key file" >&2; exit 1; }
  # Before anything is stopped or deleted: the key must open every file
  echo "Checking that the key opens the backup"
  for f in "$SRC"/*.age; do
    age -d -i "$BACKUP_AGE_IDENTITY" "$f" >/dev/null || { echo "Cannot decrypt $(basename "$f") with this key" >&2; exit 1; }
  done
fi

# Services of the active provider (COMPOSE_PROFILES in .env)
services="$("${COMPOSE[@]}" config --services)"
has() { grep -qx "$1" <<<"$services"; }

echo "Stopping application services"
app=(sam-ui sam-server)
for s in keycloak pocket-id; do has "$s" && app+=("$s"); done
"${COMPOSE[@]}" stop "${app[@]}"
dbs=(database)
if has_file keycloak.dump; then
  has keycloak-db || { echo "The backup has a Keycloak database, but the keycloak profile is not active" >&2; exit 1; }
  dbs+=(keycloak-db)
fi
if has_file pocket-id.zip; then
  has pocket-id || { echo "The backup has Pocket ID data, but the pocket-id profile is not active" >&2; exit 1; }
elif has pocket-id; then
  echo "NOTE: the backup has no Pocket ID data (older than #216); Pocket ID keeps its current data" >&2
fi
"${COMPOSE[@]}" up -d "${dbs[@]}"

restore() { # <service> <dump file in the backup>
  # Over TCP: on a new volume, the image's temporary init server answers on the socket and then stops
  until "${COMPOSE[@]}" exec -T "$1" pg_isready -q -h 127.0.0.1; do sleep 1; done
  content "$2" | "${COMPOSE[@]}" exec -T "$1" sh -c \
    'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner --exit-on-error'
}
echo "Restoring databases"
restore database sam_music.dump
if has_file keycloak.dump; then restore keycloak-db keycloak.dump; fi

if has_file pocket-id.zip; then
  # Needs the stopped service: import takes exclusive access to Pocket ID's database
  echo "Restoring Pocket ID"
  content pocket-id.zip | "${COMPOSE[@]}" run --rm -T --no-deps pocket-id /app/pocket-id import -p - -y
fi

echo "Restoring sheet music files"
content sam-data.tar.gz | "${COMPOSE[@]}" run --rm -T --no-deps restore-files sh -c \
  'find /data -mindepth 1 -delete && tar xzf - -C /data'

if [ "${RESTORE_START:-yes}" = no ]; then
  echo "Not starting the stack (RESTORE_START=no); the databases keep running"
else
  echo "Starting the stack"
  "${COMPOSE[@]}" up -d
fi
