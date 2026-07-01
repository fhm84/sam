# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SAM (Sheet music Archiving & Management) is a Quarkus-based application for archiving sheet music, managing instrumentations, musicians, and collections for bands/ensembles.

## Build & Run Commands

```bash
# Build all modules
./mvnw package

# Run server in dev mode (live reload, Dev UI at http://localhost:8080/q/dev/)
./mvnw quarkus:dev -pl server -am

# Run unit tests (all modules)
./mvnw test

# Run a single test class
./mvnw test -pl server -am -Dtest=SheetImporterTest

# Run integration tests (skipped by default via skipITs=true)
./mvnw verify -pl server -am -DskipITs=false

# Format code (Palantir Java Format via Spotless)
./mvnw spotless:apply

# Check formatting without fixing
./mvnw spotless:check

# Generate TypeScript types from API entities (opt-in via profile; skipped in normal builds)
# Profile lives in ui/pom.xml — must be run against the ui module, not api
./mvnw generate-sources -pl ui -am -Pgenerate-ts
# Or use the /sync-datamodels skill which runs this and shows the diff

# Build native executable
./mvnw package -Dnative

# ── Docker images ────────────────────────────────────────────────────────────
# Backend image (Jib — output: de.halbmann/sam:latest)
./mvnw package -Dquarkus.container-image.build=true -pl server -am

# Push to a registry (append registry/group overrides as needed)
./mvnw package -Dquarkus.container-image.build=true -Dquarkus.container-image.push=true \
  -Dquarkus.container-image.registry=ghcr.io \
  -Dquarkus.container-image.group=your-org \
  -pl server -am

# Frontend image (multi-stage Dockerfile — output: de.halbmann/sam-ui:latest)
docker build -t de.halbmann/sam-ui:latest .

# Start full production stack (copy .env.example → .env, fill in secrets first)
docker compose -f docker-compose.prod.yml up

# Scan dependencies for CVEs (OWASP Dependency-Check; first run downloads NVD DB ~200MB)
./mvnw verify -Pdependency-check
# With NVD API key for faster DB updates (free key at https://nvd.nist.gov/developers/request-an-api-key)
./mvnw verify -Pdependency-check -DnvdApiKey=YOUR_KEY
# Report output: target/dependency-check-report/dependency-check-report.html
```

## Module Architecture

Six Maven modules under parent `de.halbmann:sam`:

- **api** — JAX-RS interfaces (`SamResources` as root), DTOs/entities, and MicroProfile REST Client annotations. TypeScript types can be generated into `ui/src/main/webui/src/app/model/datamodels.d.ts` via `typescript-generator-maven-plugin` (opt-in with `-Pgenerate-ts`; does not run in normal builds because the plugin cannot handle Java records).
- **core** — Shared business logic and storage SDK integration (CDI beans, no Quarkus runtime dependency).
- **storage** — Parent POM for storage abstraction with three sub-modules: `storage-sdk` (SPI), `storage-local` (filesystem), `storage-s3` (AWS S3).
- **server** — Quarkus runtime: REST resource implementations, Hibernate/Panache entities, Flyway migrations, MapStruct mappers, LangChain4j AI integration, document management, AI-based document classification. Requires PostgreSQL.
- **ui** — Angular frontend served via Quarkus Quinoa. Source lives in `ui/src/main/webui/`.
- **cli** — Quarkus PicoCLI application for importing sheet music data. Uses the `api` module as a REST client.

## Key Patterns

- **API-first design**: REST interfaces are defined in the `api` module with `@RegisterRestClient`. The `server` module provides `*Impl` classes. The `cli` module consumes the same interfaces as a REST client.
- **Entity mapping**: MapStruct mappers in `server/src/.../business/controller/` convert between API DTOs and Hibernate/Panache entities.
- **Annotation processors**: Lombok + MapStruct are configured together in the parent POM's compiler plugin. Both must be on the annotation processor path.
- **Audit trail**: Hibernate Envers (`_AUD` tables) tracks all entity changes with `ValidityAuditStrategy`.
- **Full-text search**: PostgreSQL `tsvector`, trigram (`pg_trgm`), and phonetic (`dmetaphone`) search on sheets — see `V1.0.0__Initial_schema.sql`.
- **Fingerprinting**: `FingerprintService`/`FingerprintFactory` generate content-based fingerprints for deduplication of sheet music.
- **Sub-resources**: JAX-RS sub-resource pattern — e.g. `SheetsResource.instrumentations(sheetId)` returns `InstrumentationsResource`.
- **Exports / document generation**: All export endpoints return `ExportResult` (filename + MIME type + `StreamWriter`). The resource layer wraps it in a JAX-RS `Response`; services stay framework-agnostic. Current implementations: `SheetExportService` (JSON/CSV/ZIP), `CollectionTocService` (Qute HTML → PDF via Flying Saucer), `GemaSetlistService` (Apache POI xlsx from template). When adding a new export format, follow the same `ExportResult` pattern and keep POI/PDF rendering in the service, not the resource.
- **Resource placement for non-Qute files**: Quarkus Qute scans **everything** under `server/src/main/resources/templates/` and crashes on binary files. Place non-template resources (e.g. xlsx files) elsewhere — currently `server/src/main/resources/gema/`. Load them with `getClass().getResourceAsStream("/gema/…")`.
- **PrimeNG lazy table**: Wire `(onLazyLoad)` only — do **not** also call `load()` from `ngOnInit`. `p-table` with `[lazy]="true"` fires `onLazyLoad` on initialization, so a redundant `ngOnInit` call causes a double request on page load.
- **HTTP subscriptions**: Always pipe through `takeUntilDestroyed(this.destroyRef)` (inject `DestroyRef`) to guard against in-flight responses arriving after component destruction.
- **Document classification**: Two-step AI workflow triggered after upload. `POST /documents/{id}/classify` runs the LangChain4j analyzer and returns `SheetClassification` with detected metadata and pre-matched entity references. `POST /documents/{id}/apply` accepts a reviewed `ClassificationApplyRequest` and creates/resolves musician, instrument, sheet, and instrumentation entities, then links the document as an attachment. For PDFs, text is extracted with `PDFTextStripper` first (cheaper); only scanned/image-only PDFs fall back to GPT-4o vision. Key classes: `server/src/main/java/de/halbmann/sam/classification/`.

## Security

- **OIDC provider**: Keycloak 26 (`docker-compose.keycloak.yml`). Realm export at `keycloak/sam-realm.json`.
- **Dev Keycloak**: `docker compose -f docker-compose.keycloak.yml up` — runs on port 8180, auto-imports realm.
- **Angular OIDC config**: The Angular app fetches `/oidc-config.json` at startup. In **dev**, the static file `ui/src/main/webui/public/oidc-config.json` is served (committed with `localhost:8180` defaults). In **production**, `OidcConfigResource` (`server/.../OidcConfigResource.java`) — a `@PermitAll` JAX-RS endpoint at that same path — takes precedence and returns `{issuerUrl, clientId}` read from `quarkus.oidc.auth-server-url` / `quarkus.oidc.client-id`, which are set via `OIDC_SERVER_URL` and `OIDC_CLIENT_ID` env vars. No image rebuild needed to change the Keycloak URL.
- **Realm roles**: `admin`, `music_librarian`. Groups use `ensemble:{UUID}` naming for per-ensemble access.
- **Auth enforcement**: `@Authenticated` at class level on all `*ResourceImpl` classes (all endpoints require a token). Write methods additionally carry `@RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})`. Role constants are in `server/src/main/java/de/halbmann/sam/security/Roles.java`.
- **Do NOT put `@RolesAllowed` on the `api` module interfaces** — they are also used as REST clients by the `cli` module.
- **Test profile**: `%test.quarkus.oidc.enabled=false` — existing `@QuarkusTest` tests run without auth. Auth-specific tests use `@TestSecurity` from `quarkus-test-security`.
- **`CurrentUserService`** (`@RequestScoped`, `server/.../security/`): provides `getUserId()`, `hasRole()`, `getAccessibleEnsembleIds()`, `canAccessEnsemble(UUID)` — inject this instead of `JsonWebToken` directly in business logic.

## Monitoring

Prometheus + Grafana stack, opt-in via a separate compose file (does not affect the main app):

```bash
# Start Prometheus (:9090) and Grafana (:3000) — app must be running on :8080
docker compose -f docker-compose.monitoring.yml up

# Grafana: http://localhost:3000  (admin / admin)
# Prometheus raw metrics: http://localhost:8080/q/metrics
```

- Prometheus scrape config: `monitoring/prometheus.yml`
- Grafana dashboard (auto-provisioned): `monitoring/grafana/dashboards/sam.json`
- Dashboard covers: HTTP request rate/latency/errors, JVM heap/GC/threads, HikariCP pool, AI classification (text vs vision mode, duration), LLM token usage

## Database

- PostgreSQL with Flyway migrations in `server/src/main/resources/db/migration/`
- Dev services are disabled; a running PostgreSQL instance is expected (default: `localhost:5432/sam_music`, user `sam`)
- Extensions required: `pg_trgm`, `fuzzystrmatch`

## Code Style

- Java 21, Palantir Java Format (via Spotless plugin)
- Run `./mvnw spotless:apply` before committing

## Verification Policy

### Backend (Java / Quarkus)

After implementing any backend change:

1. **Write or update tests** — unit tests for new/changed business logic; `@QuarkusTest` integration tests for new endpoints or complex service interactions.
2. **Run the affected tests** and confirm they pass before marking the task done:
   ```bash
   # Single test class (fast)
   rtk ./mvnw test -pl server -am -Dtest=<TestClass>

   # All unit tests
   rtk ./mvnw test

   # Integration tests (requires running PostgreSQL)
   rtk ./mvnw verify -pl server -am -DskipITs=false
   ```
3. **Apply formatting** after any Java edits:
   ```bash
   ./mvnw spotless:apply
   ```
4. Do not consider code review or compilation alone as proof — tests must actually execute and pass.

### Frontend (Angular)

After implementing any Angular UI change:

1. **Run TypeScript compilation** to catch type errors:
   ```bash
   cd ui/src/main/webui && rtk npx tsc --noEmit
   ```
2. **Verify the rendered result in a real browser** using the Playwright MCP skill — do not rely on code inspection alone:
   - New features / pages → use `/angular-primeng-browser-verify`
   - Bug fixes → use `/browser-fix-and-recheck`
3. Pay special attention to: PrimeNG overlays, dialogs, tables, form validation, routing transitions, and responsive behavior.
4. Test both the golden path and relevant edge cases (empty state, validation errors, loading state).
5. **Save Playwright screenshots to `target/playwright/`** — `target/` is gitignored, so screenshots never end up in the working tree or a commit.

### When tests are not applicable

If a change is purely structural (rename, move, formatting) or documentation-only and no meaningful test can be written, state this explicitly rather than skipping silently.

### Documentation

After implementing a feature or bugfix:
1. Update README.md TODOs if the feature is now complete
2. If architecture changed, verify docs/architecture.md sections match
3. If UI patterns changed, check memory files (ui-architecture.md, etc.)
4. If adding new modules or resources, update the module structure in architecture.md
5. If a `docs/roadmap.md` item shipped, flip its status (`idea`/`planned`/`in progress` → `done`) instead of leaving it stale
6. If the change affects who can do what (new/changed `@RolesAllowed`, new endpoint, new share/visibility behavior, new personalised view), check `docs/stakeholders.md` — its use cases and the Section 6 access-control model drift silently and easily (e.g. RBAC and share links stayed marked "planned" long after they shipped)
7. If a new feature or endpoint is user-facing, check `docs/features.md` for a missing or stale entry
