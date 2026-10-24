# Monitoring

Prometheus + Grafana stack, opt-in via a separate compose file (does not affect the main app):

```bash
# Start Prometheus (:9090) and Grafana (:3000) — app must be running on :8080
# and its management port :9000 must also be reachable. In `quarkus:dev`, the
# management interface is explicitly bound to 0.0.0.0 (see application.properties)
# so Prometheus can reach it via host.docker.internal.
docker compose -f docker-compose.monitoring.yml up

# Grafana: http://localhost:3000  (admin / admin)
# Prometheus raw metrics: http://localhost:9000/q/metrics
```

- Prometheus scrape config: `monitoring/prometheus.yml`
- Grafana dashboard (auto-provisioned): `monitoring/grafana/dashboards/sam.json`
- Dashboard covers: HTTP request rate/latency/errors, JVM heap/GC/threads, HikariCP pool, AI classification (text vs vision mode, duration), LLM token usage; in production also the edge (Caddy requests, latency, traffic per site) and the host (CPU, memory, disk)

## Production

`docker-compose.prod.yml` has the same stack as the opt-in profile `monitoring`
(`COMPOSE_PROFILES=<provider>,monitoring` in `.env`, plus `GRAFANA_ADMIN_PASSWORD`):

- Prometheus (`monitoring/prometheus.prod.yml`) runs on the stack's network and scrapes
  `sam-server:9000`, Caddy (`sam-ui:9180`, a metrics site in `docker/Caddyfile` with
  per-site labels) and node-exporter (host CPU, memory, disks; network collectors are off
  because it runs in its own network namespace). Retention: 1 year or 5 GB, whichever comes
  first (about 1,300 series, roughly 4 GB a year). No ports are published.
- Grafana binds to the server's loopback only: `ssh -L 3000:localhost:3000 <server>`, then
  `http://localhost:3000`. It refuses to start without `GRAFANA_ADMIN_PASSWORD`, which applies
  on its first start only. It shares provisioning and dashboard with dev; the Edge and Host
  rows only have data in production.
- Metrics only, no alerting: nobody is notified when something breaks.
- Memory limits: Prometheus 384 MB, Grafana 256 MB, node-exporter 64 MB (idle about 30, 140
  and 10 MB).

## Management interface

`quarkus.management.enabled=true` moves all `/q/*` ops endpoints (metrics, info,
health) off the main HTTP port (:8080, shared with the public
`/api/*` surface) onto a dedicated management port (:9000). This keeps ops/diagnostic
endpoints separate from the `@Authenticated`-by-default public API. See
`docs/architecture/decisions/` for the rationale.

- `GET :9000/q/info` — running version/build-id: git commit SHA + branch, build
  timestamp, Quarkus/Java/OS versions. Unauthenticated by design; deliberately
  configured with `quarkus.info.git.mode=standard` (not `full`) so it does *not*
  expose commit author/committer name+email or the build machine's hostname.
- `GET :9000/q/health/ready` — readiness: database and writable storage. The prod compose
  healthcheck calls it and gates `sam-ui`'s start on it ([ADR-0013](../docs/architecture/decisions/adr-0013-readiness-gates-startup.md)).
- In prod, the Caddy edge proxy (`docker/Caddyfile`) answers `/q/*` with 404 — the
  management port is neither published nor proxied ([ADR-0010](../docs/architecture/decisions/adr-0010-caddy-edge-proxy.md)).
  Prometheus scrapes `sam-server:9000` over the internal network.
