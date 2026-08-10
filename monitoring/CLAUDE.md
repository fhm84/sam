# Monitoring

Prometheus + Grafana stack, opt-in via a separate compose file (does not affect the main app):

```bash
# Start Prometheus (:9090) and Grafana (:3000) — app must be running on :8080
# (management port :9000 must also be reachable — it's the default, no extra config needed)
docker compose -f docker-compose.monitoring.yml up

# Grafana: http://localhost:3000  (admin / admin)
# Prometheus raw metrics: http://localhost:9000/q/metrics
```

- Prometheus scrape config: `monitoring/prometheus.yml`
- Grafana dashboard (auto-provisioned): `monitoring/grafana/dashboards/sam.json`
- Dashboard covers: HTTP request rate/latency/errors, JVM heap/GC/threads, HikariCP pool, AI classification (text vs vision mode, duration), LLM token usage

## Management interface

`quarkus.management.enabled=true` moves all `/q/*` ops endpoints (metrics, info, and
any future health checks) off the main HTTP port (:8080, shared with the public
`/api/*` surface) onto a dedicated management port (:9000). This keeps ops/diagnostic
endpoints separate from the `@Authenticated`-by-default public API. See
`docs/architecture/decisions/` for the rationale.

- `GET :9000/q/info` — running version/build-id: git commit SHA + branch + dirty flag,
  build timestamp, Quarkus/Java/OS versions. Unauthenticated by design (no secrets).
- In prod, nginx (`docker/nginx.conf`) reverse-proxies `/q/` to `sam-server:9000`
  internally — the management port itself is not published to the host.
