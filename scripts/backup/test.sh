#!/usr/bin/env bash
# Tests backup.sh and restore.sh against a throwaway stack: the database and Pocket ID of
# docker-compose.prod.yml under their own project name, with their own volumes, and a temporary
# backup directory. Everything is removed afterwards. Run by CI (.github/workflows/scripts.yml).
#
#   scripts/backup/test.sh
#
# Needs Linux (or WSL) with bash, Docker with the compose plugin, and age. A marker in the
# database and one in the file volume show whether a restore brought the old state back.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
for c in docker age age-keygen; do
  command -v "$c" >/dev/null || { echo "$c is missing" >&2; exit 2; }
done

work="$(mktemp -d)"
log="$work/log"
export ENV_FILE="$work/env" BACKUP_DIR="$work/backups"
COMPOSE=(docker compose -f "$ROOT/docker-compose.prod.yml" --env-file "$ENV_FILE")
BACKUP=(bash "$ROOT/scripts/backup/backup.sh")
# Without starting the whole stack afterwards: the test needs neither the app nor ports 80/443
RESTORE=(env RESTORE_START=no bash "$ROOT/scripts/backup/restore.sh")

cleanup() {
  "${COMPOSE[@]}" down -v --remove-orphans >/dev/null 2>&1 || true
  rm -rf "$work"
}
trap cleanup EXIT

age-keygen -o "$work/key.txt" 2>/dev/null
age-keygen -o "$work/other-key.txt" 2>/dev/null
cat >"$ENV_FILE" <<EOF
COMPOSE_PROJECT_NAME=sam-backup-test-$$
SAM_DOMAIN=sam.localhost
LOGIN_DOMAIN=login.localhost
ACME_EMAIL=test@example.org
DB_PASS=test-$RANDOM$RANDOM
OPENAI_API_KEY=unused
SAM_OIDC_PROVIDER=pocket-id
COMPOSE_PROFILES=pocket-id
POCKET_ID_ENCRYPTION_KEY=$(head -c 32 /dev/urandom | base64)
BACKUP_AGE_RECIPIENT=$(age-keygen -y "$work/key.txt")
EOF

failures=0
pass() { echo "ok    $1"; }
fail() {
  echo "FAIL  $1"
  failures=$((failures + 1))
  if [ -s "$log" ]; then tail -n 25 "$log" | sed 's/^/      /'; fi
}
expect() { # <description> <ok|fail> <command...>: the command's outcome
  local description="$1" want="$2" got=ok
  shift 2
  "$@" >"$log" 2>&1 || got=fail
  if [ "$got" = "$want" ]; then pass "$description"; else fail "$description"; fi
}
is() { # <description> <actual> <expected>
  : >"$log"
  if [ "$2" = "$3" ]; then pass "$1"; else fail "$1 (got '$2', expected '$3')"; fi
}

set_markers() {
  "${COMPOSE[@]}" exec -T database psql -q -U sam -d sam_music \
    -c "drop table if exists marker; create table marker(v text); insert into marker values ('$1');" 2>/dev/null
  "${COMPOSE[@]}" run --rm -T --no-deps restore-files sh -c "echo $1 > /data/marker" 2>/dev/null
}
markers() { # "<database marker> <file marker>"
  echo "$("${COMPOSE[@]}" exec -T database psql -U sam -d sam_music -Atc 'select v from marker' 2>/dev/null)" \
    "$("${COMPOSE[@]}" run --rm -T --no-deps restore-files cat /data/marker 2>/dev/null)"
}
newest() { find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -name '20*' | sort | tail -n 1; }
names() { find "$1" -mindepth 1 -printf '%f\n' | sort | tr '\n' ' '; }
running() { "${COMPOSE[@]}" ps --status running --services 2>/dev/null | grep -qx "$1"; }
start() { "${COMPOSE[@]}" up -d --wait database pocket-id >"$log" 2>&1; }

echo "Starting the test stack"
start || { cat "$log"; exit 1; }
set_markers A

echo "Encrypted backup"
expect "an invalid public key fails the run" fail env BACKUP_AGE_RECIPIENT=age1notakey "${BACKUP[@]}"
is "and leaves nothing behind" "$(find "$BACKUP_DIR" -mindepth 1 2>/dev/null | wc -l)" 0
expect "backup with a public key" ok env BACKUP_GROUP="$(id -gn)" "${BACKUP[@]}"
encrypted="$(newest)"
is "holds only encrypted files and the checksums" "$(names "$encrypted")" \
  "SHA256SUMS env.age pocket-id.zip.age sam-data.tar.gz.age sam_music.dump.age "
is "every file is an age file" \
  "$(for f in "$encrypted"/*.age; do head -c 21 "$f" && echo; done | sort -u)" "age-encryption.org/v1"
expect "the checksums match the encrypted files" ok sh -c "cd '$encrypted' && sha256sum -c SHA256SUMS"
expect "env.age decrypts to the env file" ok sh -c "age -d -i '$work/key.txt' '$encrypted/env.age' | cmp - '$ENV_FILE'"
is "BACKUP_GROUP: directory and files are group-readable only" \
  "$(stat -c %a "$encrypted") $(stat -c %a "$encrypted"/* | sort -u)" "750 640"
is "no temporary directory is left" "$(find "$BACKUP_DIR" -maxdepth 1 -name '.tmp-*' | wc -l)" 0

echo "Restore of the encrypted backup"
set_markers B
expect "without a key: refuses" fail "${RESTORE[@]}" "$encrypted" --yes
expect "with another key: refuses" fail env BACKUP_AGE_IDENTITY="$work/other-key.txt" "${RESTORE[@]}" "$encrypted" --yes
is "the data is untouched" "$(markers)" "B B"
expect "Pocket ID was not stopped" ok running pocket-id
cp -r "$encrypted" "$work/damaged"
printf 'x' | dd of="$work/damaged/sam_music.dump.age" bs=1 seek=300 conv=notrunc 2>/dev/null
expect "a damaged file fails the checksum" fail env BACKUP_AGE_IDENTITY="$work/key.txt" "${RESTORE[@]}" "$work/damaged" --yes
expect "without --yes: refuses" fail env BACKUP_AGE_IDENTITY="$work/key.txt" "${RESTORE[@]}" "$encrypted"
expect "with the key: restores" ok env BACKUP_AGE_IDENTITY="$work/key.txt" "${RESTORE[@]}" "$encrypted" --yes
is "database and files are back" "$(markers)" "A A"

echo "Unencrypted backup and restore"
sed -i '/^BACKUP_AGE_RECIPIENT=/d' "$ENV_FILE"
start || fail "the stack starts again after a restore"
set_markers C
expect "backup without a key" ok "${BACKUP[@]}"
plain="$(newest)"
is "holds the plain files and the checksums" "$(names "$plain")" \
  "SHA256SUMS env pocket-id.zip sam-data.tar.gz sam_music.dump "
set_markers D
expect "restores" ok "${RESTORE[@]}" "$plain" --yes
is "database and files are back" "$(markers)" "C C"

echo "Pruning"
start || fail "the stack starts again after a restore"
expect "backup with BACKUP_KEEP=1" ok env BACKUP_KEEP=1 "${BACKUP[@]}"
is "only the newest backup is left" "$(find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d | wc -l)" 1

echo
if [ "$failures" -gt 0 ]; then
  echo "$failures check(s) failed"
  exit 1
fi
echo "All checks passed"
