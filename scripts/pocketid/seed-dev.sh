#!/usr/bin/env bash
# Seeds a fresh dev Pocket ID (docker-compose.pocketid.yml) with what SAM expects: the sam-admin and
# sam-music-librarian groups (= SAM roles, see sam.oidc.role-groups), the sam-member group for
# everyone else, the dev users, the public sam-ui client (only members of those groups may sign in),
# and the sam-cli service account with the music_librarian permission on the SAM API. Run once on an
# empty volume; start over with `docker compose -f docker-compose.pocketid.yml down -v`.
#
# Users have no passkey yet: print a one-time login link per user with
#   scripts/pocketid/seed-dev.sh login-link <username>
set -euo pipefail

BASE="${POCKET_ID_URL:-http://localhost:1411}/api"
KEY="${POCKET_ID_API_KEY:-sam-dev-static-api-key}"
CLI_SECRET="sam-cli-dev-secret-0000"
API_RESOURCE="urn:sam:api"

call() { # method path [json]
  curl -sf -H "X-API-KEY: $KEY" -H 'Content-Type: application/json' -X "$1" "$BASE$2" ${3:+-d "$3"}
}
first_id() { sed -E 's/^[^{]*\{"id":"([^"]+)".*/\1/'; }

if [[ "${1:-}" == "login-link" ]]; then
  user_id=$(call GET "/users?search=${2:?username}" | sed -E 's/.*"data":\[\{"id":"([^"]+)".*/\1/')
  token=$(call POST "/users/$user_id/one-time-access-token" '{"ttl":"1h"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
  echo "${POCKET_ID_URL:-http://localhost:1411}/lc/$token"
  exit 0
fi

# SAM-specific names: Pocket ID groups are global, and another app's `admin` must not be SAM's
admin_group=$(call POST /user-groups '{"friendlyName":"SAM admin","name":"sam-admin"}' | first_id)
librarian_group=$(call POST /user-groups '{"friendlyName":"SAM music librarian","name":"sam-music-librarian"}' | first_id)
member_group=$(call POST /user-groups '{"friendlyName":"SAM member","name":"sam-member"}' | first_id)

user() { # username first group-ids-json is-admin
  call POST /users "{\"username\":\"$1\",\"email\":\"$1@sam.local\",\"firstName\":\"$2\",\"lastName\":\"One\",\"emailVerified\":true,\"isAdmin\":$4,\"userGroupIds\":$3}" >/dev/null
}
user admin1 Admin "[\"$member_group\",\"$admin_group\"]" true
user librarian1 Librarian "[\"$member_group\",\"$librarian_group\"]" false
user musician1 Musician "[\"$member_group\"]" false

call POST /oidc/clients '{"id":"sam-ui","name":"SAM","isPublic":true,"pkceEnabled":true,"skipConsent":true,
  "isGroupRestricted":true,"callbackURLs":["http://localhost:4200","http://localhost:8080"],
  "logoutCallbackURLs":["http://localhost:4200","http://localhost:8080"]}' >/dev/null
call PUT /oidc/clients/sam-ui/allowed-user-groups \
  "{\"userGroupIds\":[\"$admin_group\",\"$librarian_group\",\"$member_group\"]}" >/dev/null

# Client-credentials tokens carry no groups; the CLI's role comes from an API permission in `scp`
call POST /oidc/clients '{"id":"sam-cli","name":"SAM CLI","isPublic":false,"callbackURLs":[],"logoutCallbackURLs":[]}' >/dev/null
call POST /oidc/clients/sam-cli/secrets "{\"secret\":\"$CLI_SECRET\"}" >/dev/null
api=$(call POST /apis "{\"name\":\"SAM API\",\"resource\":\"$API_RESOURCE\"}" | first_id)
permission=$(call PUT "/apis/$api/permissions" \
  '{"permissions":[{"key":"music_librarian","name":"music_librarian","description":"Write access"}]}' \
  | sed -E 's/.*"permissions":\[\{"id":"([^"]+)".*/\1/')
call PUT "/apis/$api/clients/sam-cli" \
  "{\"clientAccess\":true,\"clientPermissionIds\":[\"$permission\"],\"userDelegatedAccess\":false,\"userDelegatedPermissionIds\":[]}" >/dev/null

echo "Seeded. Log in with: $0 login-link admin1"
