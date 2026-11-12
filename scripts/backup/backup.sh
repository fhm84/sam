#!/usr/bin/env bash
# Nightly SAM backup: the SAM database, the active OIDC provider's data (Pocket ID's own export, or
# Keycloak's database), the sheet music files and .env.
# Writes a timestamped directory under $BACKUP_DIR and prunes old ones; a second machine (e.g. a NAS) pulls
# the result (see docs/architecture/production-setup.md). Run as root via the systemd timer.
#
#   BACKUP_DIR            target directory        (default /var/backups/sam)
#   BACKUP_KEEP           local backups to keep   (default 7)
#   BACKUP_GROUP          optional group that may read the backups (e.g. sam-backup)
#   ENV_FILE              env file for compose    (default <repo>/.env)
#   BACKUP_AGE_RECIPIENT  age public key(s), separated by commas or spaces; here or in the env
#                         file. Every file of the backup is then encrypted to them (<name>.age)
#                         and only the matching private key, which is not on this machine,
#                         opens it. Needs the `age` command.
set -euo pipefail

# ROOT, ENV_FILE and setting()
. "$(dirname "${BASH_SOURCE[0]}")/../alerts/lib.sh"
export BACKUP_DIR="${BACKUP_DIR:-/var/backups/sam}"
KEEP="${BACKUP_KEEP:-7}"
COMPOSE=(docker compose -f "$ROOT/docker-compose.prod.yml" --env-file "$ENV_FILE")

stamp="$(date +%Y-%m-%d_%H%M%S)"
tmp="$BACKUP_DIR/.tmp-$stamp"
final="$BACKUP_DIR/$stamp"

# Before any work: a wrong key or a missing `age` must not show up after the dumps are taken
age_args=()
recipients="$(setting BACKUP_AGE_RECIPIENT)"
for r in ${recipients//,/ }; do age_args+=(-r "$r"); done
if [ ${#age_args[@]} -gt 0 ]; then
  command -v age >/dev/null || { echo "ERROR: BACKUP_AGE_RECIPIENT is set, but the age command is missing" >&2; exit 1; }
  age "${age_args[@]}" </dev/null >/dev/null || { echo "ERROR: BACKUP_AGE_RECIPIENT is not a valid age public key" >&2; exit 1; }
else
  echo "NOTE: BACKUP_AGE_RECIPIENT is not set, the backup stays unencrypted" >&2
fi

mkdir -p "$tmp"
trap 'rm -rf "$tmp"' EXIT # no-op after the final rename; cleans up a failed run
umask 077

dump() { # <service> <output file>
  "${COMPOSE[@]}" exec -T "$1" sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom' >"$2"
}

# Services of the active provider (COMPOSE_PROFILES in .env). Decided by configuration, not by what
# runs: a stopped keycloak-db must fail the backup, not drop out of it.
services="$("${COMPOSE[@]}" config --services)"
files=(sam_music.dump)

echo "Dumping databases"
dump database "$tmp/sam_music.dump"
if grep -qx keycloak-db <<<"$services"; then
  dump keycloak-db "$tmp/keycloak.dump"
  files+=(keycloak.dump)
fi
if grep -qx pocket-id <<<"$services"; then
  # Pocket ID's own export: a consistent ZIP of its database and uploads, taken while it runs
  echo "Exporting Pocket ID"
  # As the owner of its data: root in the container has no capability to read other users'
  # files (docker-compose.prod.yml drops them), and Pocket ID keeps its uploads owner-only
  owner="$("${COMPOSE[@]}" exec -T pocket-id stat -c %u:%g /app/data)"
  "${COMPOSE[@]}" exec -T --user "$owner" pocket-id /app/pocket-id export -p - >"$tmp/pocket-id.zip"
  [ "$(head -c 2 "$tmp/pocket-id.zip")" = PK ] || { echo "ERROR: pocket-id.zip is not a ZIP file" >&2; exit 1; }
  files+=(pocket-id.zip)
fi

echo "Archiving sheet music files"
"${COMPOSE[@]}" run --rm -T --no-deps backup-files czf "/out/.tmp-$stamp/sam-data.tar.gz" -C /data .

# .env holds the secrets needed to bring the stack back up
install -m 600 "$ENV_FILE" "$tmp/env"

echo "Verifying"
files+=(sam-data.tar.gz)
for f in "${files[@]}"; do
  [ -s "$tmp/$f" ] || { echo "ERROR: $f is missing or empty" >&2; exit 1; }
done
gzip -t "$tmp/sam-data.tar.gz"
files+=(env)

if [ ${#age_args[@]} -gt 0 ]; then
  # After the checks above, which need the plain files. They only ever exist in the temporary
  # directory, on the disk that holds the live data anyway.
  echo "Encrypting"
  for f in "${files[@]}"; do
    age "${age_args[@]}" -o "$tmp/$f.age" "$tmp/$f"
    rm "$tmp/$f"
  done
  files=("${files[@]/%/.age}")
fi
# Over the files as stored: a copy can be checked without the private key
(cd "$tmp" && sha256sum "${files[@]}" >SHA256SUMS)

if [ -n "${BACKUP_GROUP:-}" ]; then
  chgrp -R "$BACKUP_GROUP" "$tmp"
  chmod 750 "$tmp"
  chmod 640 "$tmp"/*
fi

mv "$tmp" "$final" # atomic: a puller never sees a half-written backup
echo "Backup written to $final"

# Prune: keep the newest $KEEP (timestamped names sort chronologically)
find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -name '20*' | sort | head -n -"$KEEP" | xargs -r rm -rf
