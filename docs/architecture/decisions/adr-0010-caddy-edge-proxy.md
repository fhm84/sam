# ADR-0010: Caddy as the single edge proxy; `/q/*` not exposed publicly

**Status:** accepted

## Context

The production stack had two proxies in a row: a TLS proxy in front and nginx
inside the `sam-ui` image. Running SAM on a public server also raised two problems in the old nginx setup: the Quarkus management endpoints
(`/q/info`, `/q/metrics`, [ADR-0007](adr-0007-management-interface.md)) were
proxied to the internet, and `/oidc-config.json` was proxied to a path the server
does not serve (the JAX-RS root is `/api`), so it returned 404.

## Decision

The `sam-ui` image is built on `caddy:2` and carries the Angular build plus
`docker/Caddyfile`. That one Caddy serves the SPA (with the `index.html` fallback),
proxies `/api/*` to `sam-server`, rewrites `/oidc-config.json` to
`/api/oidc-config.json`, proxies the login hostname to Keycloak, and terminates
TLS (automatic Let's Encrypt). `/q/*` answers 404 at the edge; Prometheus scrapes
`sam-server:9000` over the internal network. nginx is removed.

## Consequences

- One proxy, one config language, one less container and hop.
- The UI image is now also the edge image: the TLS setup and the hostnames
  (`SAM_DOMAIN`, `KEYCLOAK_DOMAIN`, `ACME_EMAIL`, all from env) live in it, and
  swapping the proxy means rebuilding the UI image.
- Caddy has no default request-body limit, so large scan uploads work without extra config.
- `/q/info` (version/build id) is no longer reachable from outside; read it
  via the internal network (`docker compose exec sam-ui wget -qO- http://sam-server:9000/q/info`).
