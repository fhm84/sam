#!/usr/bin/env bash
# Health check of the production stack, run every 5 minutes by sam-check.timer (as root). Prints
# each problem as it finds it. It exits 1 while at least 2 of the last 4 runs (20 minutes) found
# a problem: a single bad run (a restart, a network hiccup) pages nobody, a service that keeps
# failing and coming back does, and the alert clears after three good runs in a row.
# sam-check.service reports each run to SAM_ALERT_CHECK_URL (report.sh). It checks that:
#
#   - Docker runs (the check doesn't start it; stop sam-check.timer for maintenance without it)
#   - every service of the active profiles runs and isn't unhealthy (one-shot services, those
#     without a restart policy, may have exited with 0)
#   - the app and the login host answer through Caddy, on this host's port 443
#   - their certificates are trusted and valid for SAM_ALERT_CERT_DAYS more days (default 14;
#     0 while Caddy issues them from its local CA: then TLS isn't verified at all)
#   - no filesystem is fuller than SAM_ALERT_DISK_PERCENT (default 85), in space or inodes
#   - the clock is synchronised
#
# Every step has a time limit; together about 2 minutes, the unit allows 4. SAM_DOMAIN,
# LOGIN_DOMAIN and the SAM_ALERT_* settings come from the environment, else from the env file
# (lib.sh).
#
#   SAM_DIR, ENV_FILE  see lib.sh
#   STATE_FILE         results of the last runs (default /run/sam-check.history; empty: report
#                      at once and write nothing, for runs by hand)
set -uo pipefail

# shellcheck source=lib.sh
. "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
STATE_FILE="${STATE_FILE-/run/sam-check.history}"
COMPOSE=(docker compose -f "$ROOT/docker-compose.prod.yml" --env-file "$ENV_FILE")

problems=()
problem() { # <message>: printed at once, so a run cut off by its time limit still says why
  problems+=("${1//$'\n'/ }")
  echo "${1//$'\n'/ }"
}
# why <exit status> <error output>: what to say about a failed step
why() {
  if [ "$1" = 124 ]; then echo "timed out"; else echo "${2:-exit status $1}"; fi
}
err="$(mktemp)"
trap 'rm -f "$err"' EXIT

app="$(setting SAM_DOMAIN)"
login="$(setting LOGIN_DOMAIN)"
cert_days="$(setting SAM_ALERT_CERT_DAYS 14)"
disk_max="$(setting SAM_ALERT_DISK_PERCENT 85)"
# A typo must not switch a check off quietly; 10# reads 08 as eight, not as a bad octal number
if [[ "$cert_days" =~ ^[0-9]+$ ]]; then
  cert_days=$((10#$cert_days))
else
  problem "SAM_ALERT_CERT_DAYS is not a whole number: $cert_days (using 14)"
  cert_days=14
fi
if [[ "$disk_max" =~ ^[0-9]+$ ]]; then
  disk_max=$((10#$disk_max))
else
  problem "SAM_ALERT_DISK_PERCENT is not a whole number: $disk_max (using 85)"
  disk_max=85
fi

# Containers. The services come from the configuration, not from what runs, so a missing
# container counts too. Docker is asked only when it runs: a docker command would start a
# stopped one through its socket.
services=""
if command -v systemctl >/dev/null && ! systemctl is-active --quiet docker.service; then
  problem "docker: not running"
else
  services="$(timeout 20 "${COMPOSE[@]}" config --services 2>"$err")"
  status=$?
  if [ $status -ne 0 ]; then
    problem "docker compose config: $(why $status "$(tail -n 3 "$err")")"
    services=""
  fi
  containers="$(timeout 20 "${COMPOSE[@]}" ps -a --format '{{.Service}} {{.Name}} {{.State}} {{.ExitCode}} {{.Health}}' 2>"$err")"
  status=$?
  [ $status -eq 0 ] || problem "docker compose ps: $(why $status "$(tail -n 3 "$err")")"
  for service in $services; do
    line="$(grep -m 1 "^$service " <<<"$containers")"
    if [ -z "$line" ]; then
      problem "$service: no container"
      continue
    fi
    read -r _ name state code health <<<"$line"
    case "$state" in
      running)
        [ "$health" != unhealthy ] || problem "$service: unhealthy"
        ;;
      exited)
        if [ "$code" != 0 ]; then
          problem "$service: exited with $code"
        elif ! policy="$(timeout 10 docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' "$name" 2>/dev/null)"; then
          problem "$service: exited, and its restart policy can't be read"
        elif [ -n "$policy" ] && [ "$policy" != no ]; then
          problem "$service: stopped"
        fi
        ;;
      *)
        problem "$service: $state"
        ;;
    esac
  done
fi

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
    problem "https://$1$2: $first"
  fi
  # Without a TLS connection the line above already says why
  if [ "$cert_days" -gt 0 ] && [ "${first%% *}" != 000 ]; then
    if ! end="$(openssl x509 -noout -enddate <<<"$out" 2>/dev/null)"; then
      problem "$1: no certificate in curl's answer"
    elif ! openssl x509 -noout -checkend $((cert_days * 86400)) <<<"$out" >/dev/null; then
      problem "$1: certificate expires ${end#notAfter=}"
    fi
  fi
}
if [ -z "$app" ] || [ -z "$login" ]; then
  problem "SAM_DOMAIN or LOGIN_DOMAIN missing in $ENV_FILE"
else
  # Proxied to sam-server's API, so it needs Caddy and sam-server
  check_host "$app" /oidc-config.json 200
  # 502/503/504 come from Caddy when the provider doesn't answer
  check_host "$login" / '[234][0-9][0-9]'
fi

# Disks: space and inodes (filesystems without inodes report "-")
disks="$(timeout 10 df -x tmpfs -x devtmpfs -x overlay -x squashfs -x efivarfs --output=pcent,ipcent,target 2>"$err")"
status=$?
[ $status -eq 0 ] || problem "disk: df $(why $status "$(tail -n 1 "$err")")"
while read -r space inodes mount; do
  for used in "$space" "$inodes"; do
    used="${used%\%}"
    if [[ "$used" =~ ^[0-9]+$ ]] && [ "$used" -gt "$disk_max" ]; then
      problem "disk: $mount is $space full ($inodes of inodes)"
      break
    fi
  done
done < <(tail -n +2 <<<"$disks")

# A clock that drifts breaks certificates and token checks
if command -v timedatectl >/dev/null; then
  [ "$(timeout 10 timedatectl show -p NTPSynchronized --value 2>/dev/null)" = yes ] ||
    problem "clock: not synchronised"
fi

summary="OK: $(wc -w <<<"$services") services, $app, $login, disks below $disk_max %"
if [ -z "$STATE_FILE" ]; then
  [ ${#problems[@]} -eq 0 ] || exit 1
  echo "$summary"
  exit 0
fi

# The last 4 runs: this one and those of the previous 20 minutes (older entries are from before
# a pause of the timer). One line per run: its time, then OK or its problems.
now="$(date +%s)"
runs=()
while read -r time result; do
  if [[ "$time" =~ ^[0-9]+$ ]] && [ $((now - time)) -le 1260 ]; then
    runs+=("$time $result")
  fi
done < <(tail -n 3 "$STATE_FILE" 2>/dev/null)
if [ ${#problems[@]} -eq 0 ]; then
  runs+=("$now OK")
else
  joined="$(printf '%s; ' "${problems[@]}")"
  runs+=("$now ${joined%; }")
fi
printf '%s\n' "${runs[@]}" | tail -n 3 >"$STATE_FILE"

bad=0
for run in "${runs[@]}"; do
  [ "${run#* }" = OK ] || bad=$((bad + 1))
done
if [ $bad -ge 2 ]; then
  echo "Problems in $bad of the last ${#runs[@]} runs:"
  for run in "${runs[@]}"; do
    echo "  $(date -d "@${run%% *}" +%H:%M) ${run#* }"
  done
  exit 1
fi
if [ ${#problems[@]} -eq 0 ]; then
  echo "$summary"
else
  echo "Reported if problems show up again within 20 minutes"
fi
