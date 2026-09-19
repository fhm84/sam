# Deployment

## Container Images

SAM ships as two Docker images:

```
                        :443
  Browser ──── Caddy (sam-ui) ──┬── /api/*           ──► sam-server:8080
                                ├── /oidc-config.json ──► sam-server:8080
                                ├── /q/*              ──► 404 (internal only)
                                └── /*                ──► Angular SPA (static, /srv)
  Browser ──── Caddy (sam-ui) ── login hostname ──────► keycloak:8080
```

| Image | Built by | Contents |
|-------|----------|----------|
| `de.halbmann/sam:latest` | Jib (`./mvnw package -Dquarkus.container-image.build=true -pl server -am`) | Quarkus server JAR — REST API, Flyway, Hibernate, LangChain4j |
| `de.halbmann/sam-ui:latest` | `docker build .` (multi-stage Dockerfile) | Built Angular SPA served by Caddy, which is also the TLS edge proxy |

Caddy is the single public entry point and lives in the `sam-ui` image (`Dockerfile`, `docker/Caddyfile`, [ADR-0010](decisions/adr-0010-caddy-edge-proxy.md)). It terminates TLS (automatic Let's Encrypt certificates), serves the SPA, proxies `/api/*` and `/oidc-config.json` to `sam-server`, and proxies the Keycloak hostname to Keycloak — the only published service (ports 80/443). All Angular API calls use relative `/api/*` paths, which Caddy forwards to the backend — no CORS configuration required.

`sam-server` also listens on a separate, unpublished management port (`9000`, [ADR-0007](decisions/adr-0007-management-interface.md)) for `/q/*` ops endpoints (`/q/info` for version/build-id, `/q/metrics` for Prometheus) — kept off the main port so they don't fall under the public API's `@Authenticated` default. The edge proxy does not forward `/q/*` (404 from outside, [ADR-0010](decisions/adr-0010-caddy-edge-proxy.md)); Prometheus scrapes it over the internal network.

## OIDC Config Endpoint

`GET /oidc-config.json` is handled by `OidcConfigResource` (a `@PermitAll` JAX-RS endpoint in the `server` module). It returns `{issuerUrl, clientId}` sourced from `quarkus.oidc.auth-server-url` / `quarkus.oidc.client-id` — which are supplied via `OIDC_SERVER_URL` / `OIDC_CLIENT_ID` env vars. The edge proxy rewrites this path to the resource's real location (`/api/oidc-config.json`, since the JAX-RS root is `/api`), so the Angular app always gets the deployment-correct Keycloak URL without requiring an image rebuild.

## Required Environment Variables

Copy `.env.example` → `.env` and fill in before running `docker compose -f docker-compose.prod.yml up`.

| Variable | Used by | Default | Description |
|----------|---------|---------|-------------|
| `DB_PASS` | sam-server | — | PostgreSQL password |
| `OIDC_SERVER_URL` | sam-server | — | Full Keycloak realm URL (e.g. `https://kc.example.com/realms/sam`) |
| `OIDC_CLIENT_ID` | sam-server | `sam-ui` | OIDC client ID |
| `KEYCLOAK_ADMIN_URL` | sam-server | — | Keycloak base URL for admin REST client |
| `KEYCLOAK_BACKEND_CLIENT_SECRET` | sam-server | — | Service account secret for user search |
| `OPENAI_API_KEY` | sam-server | — | OpenAI key for document classification |
| `DB_USER` | sam-server, database | `sam` | PostgreSQL user |
| `KEYCLOAK_REALM` | sam-server | `sam` | Keycloak realm name |
| `KEYCLOAK_BACKEND_CLIENT_ID` | sam-server | `sam-backend` | Service account client ID |
| `SAM_FILESYSTEM_BASE_PATH` | sam-server | `/data/sam` | Mount point for sheet music file storage |

## Storage Volume

Sheet music files are stored in a named Docker volume (`sam-data`) mounted at `/data/sam` inside the container. The path is configurable via `SAM_FILESYSTEM_BASE_PATH`. The production compose file includes Keycloak (with its own Postgres) and imports `keycloak/prod/sam-realm.json` on first boot — a realm without dev users or dev secrets. See [Production Setup](production-setup.md).

## Related

- [Production Setup](production-setup.md) — server, DNS, backups and go-live checklist
- [Security](concepts/security.md) — OIDC / Keycloak setup
- [Storage & Deduplication](concepts/storage-and-deduplication.md) — local vs S3 backend selection
