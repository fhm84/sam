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

- Ops/diagnostic endpoints get network-level isolation from `/api/*` onto a
  separate port, rather than living behind a mix of `@Authenticated` JAX-RS
  resources and un-annotated Vert.x routes on the same port. Note
  `@Authenticated` only ever applies to `*ResourceImpl` JAX-RS classes — the
  management port doesn't "carve out" an existing protection, since
  `/q/metrics` (a raw Vert.x route registered by `quarkus-micrometer`) was
  never covered by it and was already unauthenticated on `:8080`. The real
  benefit is a single, deliberately-isolated unauthenticated surface instead
  of an implicit one mixed into the main port.
- `/q/metrics` moved with it — `monitoring/prometheus.yml` and
  `docker/nginx.conf`'s `/q/` proxy target were updated from `:8080` to
  `:9000` accordingly (see [Monitoring](../../../monitoring/CLAUDE.md)).
- The management port is not published to the host in
  `docker-compose.prod.yml`; nginx reaches it over the internal Docker
  network only, so `/q/*` is still only reachable through the existing
  public entry point (port 80).
- Minor info disclosure (exact versions aid CVE matching by an attacker) is
  accepted as low-risk. Dependabot/dependency-check reduce how often a known
  CVE is *merged*, but deploys are manual (see the `docker-build` skill) so
  they say nothing about whether the *running* instance is actually patched
  — this endpoint should not be read as implying that guarantee.
- `quarkus.info.git.mode` is deliberately `standard`, not `full`: full mode
  additionally exposes commit author/committer name+email and the build
  machine's hostname, which is more than this endpoint should leak to
  anonymous callers.
