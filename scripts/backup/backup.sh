#!/usr/bin/env bash
# Nightly SAM backup: both Postgres databases, the sheet music files and .env.
# Writes a timestamped directory under $BACKUP_DIR and prunes old ones; a second machine (e.g. a NAS) pulls
# the result (see docs/architecture/production-setup.md). Run as root via the systemd timer.
#
#   BACKUP_DIR    target directory        (default /var/backups/sam)
#   BACKUP_KEEP   local backups to keep   (default 7)
#   BACKUP_GROUP  optional group that may read the backups (e.g. sam-backup)
#   ENV_FILE      env file for compose    (default <repo>/.env)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
export BACKUP_DIR="${BACKUP_DIR:-/var/backups/sam}"
KEEP="${BACKUP_KEEP:-7}"
ENV_FILE="${ENV_FILE:-$ROOT/.env}"
COMPOSE=(docker compose -f "$ROOT/docker-compose.prod.yml" --env-file "$ENV_FILE")

stamp="$(date +%Y-%m-%d_%H%M%S)"
tmp="$BACKUP_DIR/.tmp-$stamp"
final="$BACKUP_DIR/$stamp"

mkdir -p "$tmp"
trap 'rm -rf "$tmp"' EXIT # no-op after the final rename; cleans up a failed run
umask 077

dump() { # <service> <output file>
  "${COMPOSE[@]}" exec -T "$1" sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom' >"$2"
}

echo "Dumping databases"
dump database "$tmp/sam_music.dump"
dump keycloak-db "$tmp/keycloak.dump"

echo "Archiving sheet music files"
"${COMPOSE[@]}" run --rm -T --no-deps backup-files czf "/out/.tmp-$stamp/sam-data.tar.gz" -C /data .

# .env holds the secrets needed to bring the stack back up
install -m 600 "$ENV_FILE" "$tmp/env"

echo "Verifying"
for f in sam_music.dump keycloak.dump sam-data.tar.gz; do
  [ -s "$tmp/$f" ] || { echo "ERROR: $f is missing or empty" >&2; exit 1; }
done
gzip -t "$tmp/sam-data.tar.gz"
(cd "$tmp" && sha256sum sam_music.dump keycloak.dump sam-data.tar.gz env >SHA256SUMS)

if [ -n "${BACKUP_GROUP:-}" ]; then
  chgrp -R "$BACKUP_GROUP" "$tmp"
  chmod 750 "$tmp"
  chmod 640 "$tmp"/*
fi

mv "$tmp" "$final" # atomic: a puller never sees a half-written backup
echo "Backup written to $final"

# Prune: keep the newest $KEEP (timestamped names sort chronologically)
find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -name '20*' | sort | head -n -"$KEEP" | xargs -r rm -rf
