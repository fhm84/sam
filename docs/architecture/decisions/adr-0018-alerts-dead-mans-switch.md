# ADR-0018: Alerts through an outside dead man's switch

**Status:** accepted

## Context

The production stack ran without alerts: Prometheus and Grafana record metrics, but nobody was
told about a failed backup, a full disk, an expiring certificate, an unhealthy container, or a
server that was simply off (a power cut, the line down, a crash). The last kind matters most
for a single server at home, and the server can't report it itself. Grafana alerting would
need a mail server or a webhook, and it falls silent together with the server. One person runs
the stack, so notifications have to be few and come with the reason.

## Decision

- Alerts go through **Healthchecks' ping API** (healthchecks.io's free plan, or a self-hosted
  Healthchecks elsewhere): the server reports regularly, and the service notifies when a
  report says *failed* or when reports stop arriving within the expected period.
- Two reporters, both systemd units on the host, both optional (no URL in `.env`, no report):
  - `sam-check.timer`, every 5 minutes: `scripts/alerts/check.sh` checks the containers, the
    app and the login host through Caddy, certificate trust and remaining days, disk space and
    inodes, clock sync, and that Docker runs (without starting it). It reports a failure while
    at least 2 of the last 4 runs (20 minutes) found a problem: one bad run, such as a
    restart, doesn't count, a service that keeps failing and coming back does, and the alert
    clears after three good runs in a row instead of flickering. Every step has a time limit,
    so a hanging Docker is reported, not waited for.
  - `sam-backup.service` reports each nightly run.
- `scripts/alerts/report.sh` runs as the units' `ExecStopPost`: it sends the run's result and
  log, so the notification says what is wrong. A failed report never fails the unit, and a
  run cut off by a shutdown isn't reported as failed.
- The check is plain shell on the host (`curl`, `openssl`, `df`, `timedatectl`), not part of
  the compose stack: it has to work when Docker doesn't.
- No alert rules on Prometheus metrics for now.

## Consequences

- A server, line or power outage is noticed within about 15 minutes (5-minute period plus
  10 minutes of grace), without anything on the server having to work.
- The server holds only ping URLs: they can report, not read anything or reach the server.
  Someone who has them could send false reports.
- The view from outside (DNS, port forwarding, the certificate as the internet sees it) isn't
  covered: that needs an outside uptime check of the public URL at go-live.
- Slow degradation that only metrics show (memory pressure, rising error rates) stays a
  matter of looking at Grafana.
