#!/usr/bin/env bash
# Reports how a run of a systemd service ended to a Healthchecks-compatible ping URL
# (healthchecks.io, or a self-hosted Healthchecks): the URL itself after a successful run,
# <URL>/fail otherwise, with the run's last log lines as the body. The service then notifies
# when a run fails, and also when the reports stop coming (server, power or line down).
#
# Called from a unit's ExecStopPost with the name of the setting that holds the URL, e.g.
#   ExecStopPost=-/usr/bin/bash /opt/sam/scripts/alerts/report.sh SAM_ALERT_CHECK_URL
# Without that setting it reports nothing, so alerts stay optional. SAM_DIR, ENV_FILE: lib.sh.
set -uo pipefail

# shellcheck source=lib.sh
. "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

key="${1:?usage: report.sh <setting with the ping URL>}"
if [ -z "${!key:-}" ] && [ ! -r "$ENV_FILE" ]; then
  echo "report.sh: can't read $ENV_FILE, not reported" >&2
  exit 1
fi
url="$(setting "$key")"
if [ -z "$url" ]; then
  echo "report.sh: $key is not set, not reported" >&2
  exit 0
fi

# A run cut off by a shutdown (a reboot during the backup) didn't fail. No report: if it is
# missed, the check shows it as late once its grace time is over.
if [ "${SERVICE_RESULT:-}" = signal ] && [ "$(systemctl is-system-running 2>/dev/null)" = stopping ]; then
  echo "report.sh: the system is shutting down, not reported" >&2
  exit 0
fi

# systemd hands ExecStopPost the run's result and invocation ID
body="$(
  journalctl --sync 2>/dev/null
  journalctl --no-pager -o cat "_SYSTEMD_INVOCATION_ID=${INVOCATION_ID:-none}" 2>/dev/null | tail -c 10000
)"
if [ "${SERVICE_RESULT:-}" = success ]; then
  suffix=""
else
  suffix=/fail
  body="Result: ${SERVICE_RESULT:-unknown} (${EXIT_CODE:-} ${EXIT_STATUS:-})"$'\n'"$body"
fi

curl -fsS --max-time 10 --retry 3 --data-binary @- "$url$suffix" <<<"$body" >/dev/null
