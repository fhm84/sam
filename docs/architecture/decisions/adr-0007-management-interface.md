# ADR-0007: Separate management interface for ops endpoints

**Status:** accepted

## Context

We needed a way to identify the concrete version/build of a running
deployment (git commit, build time) for debugging and support — a `GET
/q/info` endpoint via the `quarkus-info` extension. The question was where
to mount it: alongside the versioned public `/api/*` surface (where every
`*ResourceImpl` is `@Authenticated` by convention), or elsewhere.

## Decision

Enable Quarkus's management interface (`quarkus.management.enabled=true`,
port `9000`). All `/q/*` endpoints — `/q/info`, `/q/metrics`, and any future
health checks — move off the main HTTP port (`8080`, shared with `/api/*`)
onto this dedicated port. `/q/info` is intentionally left unauthenticated:
it exposes git commit/branch, build timestamp, and Quarkus/Java/OS
versions — no secrets or business data.

## Consequences

- `/api/*` stays uniformly `@Authenticated`; ops/diagnostic endpoints no
  longer need a carve-out from that rule.
- `/q/metrics` moved with it — `monitoring/prometheus.yml` and
  `docker/nginx.conf`'s `/q/` proxy target were updated from `:8080` to
  `:9000` accordingly (see [Monitoring](../../../monitoring/CLAUDE.md)).
- The management port is not published to the host in
  `docker-compose.prod.yml`; nginx reaches it over the internal Docker
  network only, so `/q/*` is still only reachable through the existing
  public entry point (port 80).
- Minor info disclosure (exact versions aid CVE matching by an attacker) is
  accepted as low-risk, consistent with running Dependabot/dependency-check
  already.
