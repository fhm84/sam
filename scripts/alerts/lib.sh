# shellcheck shell=bash
# Shared by check.sh and report.sh (sourced): where the checkout and its env file are, and how
# a setting is read.
#
#   SAM_DIR   checkout with docker-compose.prod.yml  (default: the repo this file is in)
#   ENV_FILE  its env file                           (default $SAM_DIR/.env)

ROOT="${SAM_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
ENV_FILE="${ENV_FILE:-$ROOT/.env}"

# setting <key> [default]: from the environment, else the last KEY=value line of the env file,
# else the default. A value in the file is read like compose reads a plain one: surrounding
# spaces and quotes go, and so does a " # comment" after an unquoted value. ${...} isn't
# expanded, so keep the settings read here plain values.
setting() {
  local value="${!1:-}"
  if [ -z "$value" ] && [ -r "$ENV_FILE" ]; then
    value="$(sed -n "s/^[[:space:]]*$1[[:space:]]*=//p" "$ENV_FILE" | tail -n 1)"
    value="${value#"${value%%[![:space:]]*}"}"
    case "$value" in
      \"*) value="${value#\"}" && value="${value%%\"*}" ;;
      \'*) value="${value#\'}" && value="${value%%\'*}" ;;
      *) value="${value%%[[:space:]]#*}" && value="${value%"${value##*[![:space:]]}"}" ;;
    esac
  fi
  echo "${value:-${2:-}}"
}
