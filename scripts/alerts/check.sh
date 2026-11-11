#!/usr/bin/env bash
# Health check of the production stack, run every 5 minutes by sam-check.timer (as root). Prints
# one line per problem. It exits 1 only when a problem was also there in the previous run, so a
# restart or a short network hiccup doesn't page anyone; sam-check.service then reports the run
# to SAM_ALERT_CHECK_URL (report.sh). It checks that:
#
#   - every service of the active profiles runs and isn't unhealthy (one-shot services, those
#     without a restart policy, may have exited with 0)
#   - the app and the login host answer through Caddy, on this host's port 443
#   - their certificates are trusted and valid for SAM_ALERT_CERT_DAYS more days (default 14;
#     0 while Caddy issues them from its local CA: then TLS isn't verified at all)
#   - no filesystem is fuller than SAM_ALERT_DISK_PERCENT (default 85), in space or inodes
#   - the clock is synchronised
#
# SAM_DOMAIN, LOGIN_DOMAIN and the SAM_ALERT_* settings come from the environment, else from the
# env file (lib.sh).
#
#   SAM_DIR, ENV_FILE  see lib.sh
#   STATE_FILE         problems of the last run (default /run/sam-check.problems; empty: report
#                      at once and write nothing, for runs by hand)
set -uo pipefail

# shellcheck source=lib.sh
. "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
STATE_FILE="${STATE_FILE-/run/sam-check.problems}"
COMPOSE=(docker compose -f "$ROOT/docker-compose.prod.yml" --env-file "$ENV_FILE")

# problem <key> <message>: the key names what is wrong (a service, a host, a disk) and stays the
# same from run to run, while the message may change (a percentage, an error text)
keys=()
problems=()
problem() {
  keys+=("$1")
  problems+=("$2")
}

app="$(setting SAM_DOMAIN)"
login="$(setting LOGIN_DOMAIN)"
cert_days="$(setting SAM_ALERT_CERT_DAYS 14)"
disk_max="$(setting SAM_ALERT_DISK_PERCENT 85)"
# A typo must not switch a check off quietly
if ! [[ "$cert_days" =~ ^[0-9]+$ ]]; then
  problem setting:SAM_ALERT_CERT_DAYS "SAM_ALERT_CERT_DAYS is not a whole number: $cert_days (using 14)"
  cert_days=14
fi
if ! [[ "$disk_max" =~ ^[0-9]+$ ]]; then
  problem setting:SAM_ALERT_DISK_PERCENT "SAM_ALERT_DISK_PERCENT is not a whole number: $disk_max (using 85)"
  disk_max=85
fi

# Containers. The services come from the configuration, not from what runs, so a missing
# container counts too. Every call has a time limit: a hanging Docker is a problem to report,
# not a reason to hang.
if ! services="$(timeout 30 "${COMPOSE[@]}" config --services 2>/dev/null)"; then
  error="$(timeout 30 "${COMPOSE[@]}" config --services 2>&1 >/dev/null | tail -n 3)"
  problem compose "docker compose config: ${error:-timed out}"
  services=""
fi
if ! containers="$(timeout 30 "${COMPOSE[@]}" ps -a --format '{{.Service}} {{.Name}} {{.State}} {{.ExitCode}} {{.Health}}' 2>&1)"; then
  problem compose "docker compose ps: ${containers:-timed out}"
fi
for service in $services; do
  line="$(grep -m 1 "^$service " <<<"$containers")"
  if [ -z "$line" ]; then
    problem "service:$service" "$service: no container"
    continue
  fi
  read -r _ name state code health <<<"$line"
  case "$state" in
    running)
      [ "$health" != unhealthy ] || problem "service:$service" "$service: unhealthy"
      ;;
    exited)
      policy="$(timeout 10 docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' "$name" 2>/dev/null)"
      if [ "$code" != 0 ]; then
        problem "service:$service" "$service: exited with $code"
      elif [ -n "$policy" ] && [ "$policy" != no ]; then
        problem "service:$service" "$service: stopped"
      fi
      ;;
    *)
      problem "service:$service" "$service: $state"
      ;;
  esac
done

# Through Caddy, as a browser would reach it, but on this host. curl hands back the status, its
# error if any, and the certificates it was served, so one request checks all three.
tls=()
[ "$cert_days" -gt 0 ] || tls=(-k)
check_host() { # <host> <path> <expected status, a regex>
  local out first end
  out="$(curl -s -o /dev/null -w '%{http_code} %{errormsg}\n%{certs}' --max-time 15 -A sam-check \
    "${tls[@]}" --resolve "$1:443:127.0.0.1" "https://$1$2")"
  first="${out%%$'\n'*}"
  if ! [[ "${first%% *}" =~ ^($3)$ ]]; then
    problem "https:$1" "https://$1$2: $first"
  fi
  # Without a TLS connection the line above already says why
  if [ "$cert_days" -gt 0 ] && [ "${first%% *}" != 000 ]; then
    if ! end="$(openssl x509 -noout -enddate <<<"$out" 2>/dev/null)"; then
      problem "cert:$1" "$1: no certificate in curl's answer"
    elif ! openssl x509 -noout -checkend $((cert_days * 86400)) <<<"$out" >/dev/null; then
      problem "cert:$1" "$1: certificate expires ${end#notAfter=}"
    fi
  fi
}
if [ -z "$app" ] || [ -z "$login" ]; then
  problem setting:domains "SAM_DOMAIN or LOGIN_DOMAIN missing in $ENV_FILE"
else
  # Proxied to sam-server's API, so it needs Caddy and sam-server
  check_host "$app" /oidc-config.json 200
  # 502/503/504 come from Caddy when the provider doesn't answer
  check_host "$login" / '[234][0-9][0-9]'
fi

# Disks: space and inodes (filesystems without inodes report "-")
while read -r space inodes mount; do
  for used in "$space" "$inodes"; do
    used="${used%\%}"
    if [[ "$used" =~ ^[0-9]+$ ]] && [ "$used" -gt "$disk_max" ]; then
      problem "disk:$mount" "disk: $mount is $space full ($inodes of inodes)"
      break
    fi
  done
done < <(timeout 10 df -x tmpfs -x devtmpfs -x overlay -x squashfs -x efivarfs --output=pcent,ipcent,target 2>/dev/null | tail -n +2)

# A clock that drifts breaks certificates and token checks
if command -v timedatectl >/dev/null; then
  [ "$(timeout 10 timedatectl show -p NTPSynchronized --value 2>/dev/null)" = yes ] ||
    problem clock "clock: not synchronised"
fi

if [ ${#problems[@]} -eq 0 ]; then
  [ -z "$STATE_FILE" ] || rm -f "$STATE_FILE"
  echo "OK: $(wc -w <<<"$services") services, $app, $login, disks below $disk_max %"
  exit 0
fi
printf '%s\n' "${problems[@]}"
[ -n "$STATE_FILE" ] || exit 1
previous="$(cat "$STATE_FILE" 2>/dev/null)"
printf '%s\n' "${keys[@]}" >"$STATE_FILE"
for key in "${keys[@]}"; do
  if grep -qxF -- "$key" <<<"$previous"; then
    exit 1
  fi
done
echo "New since the last run: reported if still there in the next run"
