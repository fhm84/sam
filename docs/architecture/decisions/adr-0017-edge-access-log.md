# ADR-0017: Edge access log with client IPs, kept 14 days

**Status:** accepted

## Context

Caddy, the single public entry point ([ADR-0010](adr-0010-caddy-edge-proxy.md)), logged no
requests. After an incident (a leaked share link, a login being probed, a scan) nothing showed
what was requested, when and from where. Several secrets travel in URLs: share tokens in the
path, Pocket ID's login links, and the OIDC `code` and `state` on the app's return URL, which
also reach the Referer of the SPA's next requests and the login host's redirect. The event log
deliberately holds no IP addresses ([ADR-0004](adr-0004-no-ip-logging.md)); its purpose is
attribution of downloads over 12 months, not incident response.

## Decision

- Both sites log every request to `/var/log/caddy/access.log` on the `caddy-logs` volume:
  JSON, rolled at midnight (a fixed time; an interval would start over with every container
  restart), gzipped, deleted once older than 12 days at a later roll: no line is older than
  **14 days**. Caddy 2.11 or later (`roll_at`), which the Dockerfile pins.
- Client IPs are kept complete: tracing and blocking a client is the point of the log, and
  14 days limit what it holds.
- Before a line is written, Caddy's log filters replace URL secrets with `REDACTED` in the
  request URI, the Referer and the `Location` of redirects: the paths `/share/`,
  `/api/public/shares/`, `/lc/`, `/st/` (Pocket ID's sign-up links) and
  `/api/one-time-access-token/`, and the query parameters `code`, `state`, `session_state`,
  `session_code`, `key`, `token`, `access_token`, `id_token_hint` and `login_hint`, also when
  they are percent-encoded inside another parameter (a login redirect carries the original
  request). Caddy blanks the `Authorization` and `Cookie` headers by default.
- The log stays on the server and is not part of the backup.

## Consequences

- The privacy policy has to mention the access log, its IP addresses and the 14 days
  (template in [production setup](../production-setup.md#privacy-policy-note)).
- A new secret in a URL (a new token route, a provider's new parameter) leaks into the log
  until it is added to the `redact_url` snippet in `docker/Caddyfile`.
- Reading the log needs `docker compose exec` or root on the server; there is no search UI.
  Alerts don't read it ([monitoring](../../../monitoring/CLAUDE.md)).
- Requests of monitoring and health checks are logged like any other: skipping them by a
  header would let anyone hide from the log by sending that header.
