# Deployment

## Container Images

SAM ships as two Docker images:

```
                        :443
  Browser ──── Caddy (sam-ui) ──┬── /api/*           ──► sam-server:8080
                                ├── /oidc-config.json ──► sam-server:8080
                                ├── /q/*              ──► 404 (internal only)
                                └── /*                ──► Angular SPA (static, /srv)
  Browser ──── Caddy (sam-ui) ── login hostname ──────► oidc:8080 (Pocket ID or Keycloak; /admin → 404)
```

| Image | Built by | Contents |
|-------|----------|----------|
| `ghcr.io/fhm84/sam` | Jib (`./mvnw package -Dquarkus.container-image.build=true -pl server -am`) | Quarkus server JAR — REST API, Flyway, Hibernate, LangChain4j |
| `ghcr.io/fhm84/sam-ui` | `docker build .` (multi-stage Dockerfile) | Built Angular SPA served by Caddy, which is also the TLS edge proxy |

CI publishes both images publicly to the GitHub Container Registry, for `linux/amd64` and
`linux/arm64` ([ADR-0012](decisions/adr-0012-public-images-on-ghcr.md)): every push to
`main` that passes the tests gets `latest` and `sha-<short>`, and a `v*` tag gets the version
(e.g. `1.2.0`) and `sha-<short>`. Local builds use the same names, so
`docker-compose.prod.yml` runs either; it takes the tag from `SAM_IMAGE_TAG` (default `latest`).

Caddy is the single public entry point and lives in the `sam-ui` image (`Dockerfile`, `docker/Caddyfile`, [ADR-0010](decisions/adr-0010-caddy-edge-proxy.md)). It terminates TLS (automatic Let's Encrypt certificates), serves the SPA, proxies `/api/*` and `/oidc-config.json` to `sam-server`, and proxies the login hostname to the active OIDC provider (Pocket ID or Keycloak, both reachable as `oidc:8080`; `/admin` answers 404) — the only published service (ports 80/443). All Angular API calls use relative `/api/*` paths, which Caddy forwards to the backend — no CORS configuration required.

`sam-server` also listens on a separate, unpublished management port (`9000`, [ADR-0007](decisions/adr-0007-management-interface.md)) for `/q/*` ops endpoints (`/q/info` for version/build-id, `/q/metrics` for Prometheus, `/q/health` for health checks) — kept off the main port so they don't fall under the public API's `@Authenticated` default. The edge proxy does not forward `/q/*` (404 from outside, [ADR-0010](decisions/adr-0010-caddy-edge-proxy.md)); Prometheus scrapes it over the internal network.

The optional `monitoring` profile (`COMPOSE_PROFILES=<provider>,monitoring`) adds Prometheus, Grafana and node-exporter: Prometheus scrapes `sam-server:9000`, Caddy's metrics site (`:9180`, not published) and the host metrics; Grafana listens on the server's loopback only and is opened through an SSH tunnel. Details: [Monitoring](../../monitoring/CLAUDE.md).

`sam-server`'s compose healthcheck calls `/q/health/ready` (database reachable, storage writable), and `sam-ui` starts only once it passes, so `docker compose up -d` waits for the server and fails if it stays unhealthy ([ADR-0013](decisions/adr-0013-readiness-gates-startup.md)). `docker inspect sam-server` (`.State.Health.Log`) shows which check is down.

## OIDC Config Endpoint

`GET /oidc-config.json` is handled by `OidcConfigResource` (a `@PermitAll` JAX-RS endpoint in the `server` module). It returns `{issuerUrl, clientId, scope, roleClaimPath, roleGroups}`; issuer and client ID are sourced from `quarkus.oidc.auth-server-url` / `quarkus.oidc.client-id` — which are supplied via `OIDC_SERVER_URL` / `OIDC_CLIENT_ID` env vars. The edge proxy rewrites this path to the resource's real location (`/api/oidc-config.json`, since the JAX-RS root is `/api`), so the Angular app always gets the deployment-correct provider URL without requiring an image rebuild.

## Required Environment Variables

Copy `.env.example` → `.env` and fill in before running `docker compose -f docker-compose.prod.yml up`.

| Variable | Used by | Default | Description |
|----------|---------|---------|-------------|
| `SAM_DOMAIN`, `LOGIN_DOMAIN`, `ACME_EMAIL` | sam-ui | — | Public hostnames of the app and the login provider; Let's Encrypt contact |
| `DB_PASS` | sam-server | — | PostgreSQL password |
| `SAM_OIDC_PROVIDER` | sam-server | `pocket-id` | `pocket-id` or `keycloak` |
| `COMPOSE_PROFILES` | compose | — | `${SAM_OIDC_PROVIDER}`: starts only the active provider's containers |
| `OIDC_SERVER_URL` | sam-server | `https://<LOGIN_DOMAIN>` | Issuer URL; Keycloak: `https://<LOGIN_DOMAIN>/realms/sam` |
| `OIDC_CLIENT_ID` | sam-server | `sam-ui` | OIDC client ID |
| `POCKET_ID_ENCRYPTION_KEY` | pocket-id | — | Pocket ID only, required: encrypts its stored keys; never change it |
| `SAM_OIDC_POCKET_ID_API_KEY` | sam-server | — | Pocket ID only, optional: API key for user search when linking musicians. Full admin rights on Pocket ID — see [security](concepts/security.md) for rotation; without it, musicians are linked by user ID |
| `SAM_OIDC_POCKET_ID_URL` | sam-server | `OIDC_SERVER_URL` | Pocket ID only: base URL for its API; the prod compose sets the internal `http://pocket-id:8080` |
| `KEYCLOAK_ADMIN_URL` | sam-server | `http://keycloak:8080` | Keycloak only: base URL for the admin REST client |
| `KEYCLOAK_BACKEND_CLIENT_SECRET` | sam-server, keycloak | — | Keycloak only: service account secret for user search |
| `KEYCLOAK_CLI_CLIENT_SECRET`, `KEYCLOAK_ADMIN_PASSWORD`, `KEYCLOAK_DB_PASS` | keycloak | — | Keycloak only: `cli` secret, bootstrap admin, Keycloak's database |
| `OPENAI_API_KEY` | sam-server | — | OpenAI key for document classification |
| `DB_USER` | sam-server, database | `sam` | PostgreSQL user |
| `KEYCLOAK_REALM` | sam-server | `sam` | Keycloak only: realm name |
| `KEYCLOAK_BACKEND_CLIENT_ID` | sam-server | `sam-backend` | Keycloak only: service account client ID |
| `SAM_FILESYSTEM_BASE_PATH` | sam-server | `/data/sam` | Mount point for sheet music file storage |
| `GRAFANA_ADMIN_PASSWORD` | grafana | — | `monitoring` profile only, required there: Grafana's admin password (first start only) |
| `SAM_IMAGE_TAG` | compose | `latest` | Image tag, e.g. a pinned `sha-<short>` or a version |

## Storage Volume

Sheet music files are stored in a named Docker volume (`sam-data`) mounted at `/data/sam` inside the container. The path is configurable via `SAM_FILESYSTEM_BASE_PATH`. The server runs as uid 185, but a new volume belongs to root: the one-shot `sam-data-init` service hands the volume's root to uid 185 on every `up`, before the server starts. The production compose file runs Pocket ID (data in the `pocket-id-data` volume) or, with the `keycloak` profile, Keycloak with its own Postgres, which imports `keycloak/prod/sam-realm.json` on first boot — a realm without dev users or dev secrets. See [Production Setup](production-setup.md).

## Related

- [Production Setup](production-setup.md) — server, DNS, backups and go-live checklist
- [Security](concepts/security.md) — OIDC providers, user search, API key rotation
- [Storage & Deduplication](concepts/storage-and-deduplication.md) — local vs S3 backend selection
