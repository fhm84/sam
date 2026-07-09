# Deployment

## Container Images

SAM ships as two Docker images:

```
                        :80
  Browser ──── nginx (sam-ui) ──┬── /api/*           ──► sam-server:8080
                                ├── /q/*              ──► sam-server:8080
                                ├── /oidc-config.json ──► sam-server:8080
                                └── /*                ──► Angular SPA (static)
```

| Image | Built by | Contents |
|-------|----------|----------|
| `de.halbmann/sam:latest` | Jib (`./mvnw package -Dquarkus.container-image.build=true -pl server -am`) | Quarkus server JAR — REST API, Flyway, Hibernate, LangChain4j |
| `de.halbmann/sam-ui:latest` | `docker build .` (multi-stage Dockerfile) | Built Angular SPA served by nginx |

The nginx reverse proxy is the single public entry point (port 80). All Angular API calls use relative `/api/*` paths, which nginx forwards to the backend — no CORS configuration required.

## OIDC Config Endpoint

`GET /oidc-config.json` is handled by `OidcConfigResource` (a `@PermitAll` JAX-RS endpoint in the `server` module). It returns `{issuerUrl, clientId}` sourced from `quarkus.oidc.auth-server-url` / `quarkus.oidc.client-id` — which are supplied via `OIDC_SERVER_URL` / `OIDC_CLIENT_ID` env vars. nginx proxies this path to the backend, so the Angular app always gets the deployment-correct Keycloak URL without requiring an image rebuild.

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

Sheet music files are stored in a named Docker volume (`sam-data`) mounted at `/data/sam` inside the container. The path is configurable via `SAM_FILESYSTEM_BASE_PATH`. Keycloak is expected as an external service and is not part of the production compose file.

## Related

- [Security](concepts/security.md) — OIDC / Keycloak setup
- [Storage & Deduplication](concepts/storage-and-deduplication.md) — local vs S3 backend selection
