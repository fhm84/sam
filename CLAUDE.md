# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SAM (Sheet music Archiving & Management) is a Quarkus-based application for archiving sheet music, managing instrumentations, musicians, and collections for bands/ensembles.

## Build & Run Commands

```bash
# Build all modules
./mvnw package

# Run server in dev mode (live reload, Dev UI at http://localhost:8080/q/dev/)
# Do NOT add -am: the dev goal then runs on every reactor module and fails on `api`
# ("Failed to locate quarkus-core-deployment"). Upstream modules come from the local
# repo — after changing them, run `./mvnw install -pl api -DskipTests` (etc.) first.
./mvnw quarkus:dev -pl server

# Run unit tests (all modules)
./mvnw test

# Run a single test class (the flag keeps upstream modules without a match from failing)
./mvnw test -pl server -am -Dtest=SheetImporterTest -Dsurefire.failIfNoSpecifiedTests=false

# Run integration tests (skipped by default via skipITs=true)
./mvnw verify -pl server -am -DskipITs=false

# Format code (Palantir Java Format via Spotless)
./mvnw spotless:apply

# Check formatting without fixing
./mvnw spotless:check

# Generate TypeScript types from API entities — see the /sync-datamodels skill (also shows the diff)

# Build native executable
./mvnw package -Dnative

# Build/push Docker images or start the production stack — see the docker-build skill
# Scan dependencies for CVEs — see the dependency-check skill
```

## Module Architecture

Seven Maven modules under parent `de.halbmann:sam`:

- **api** — JAX-RS interfaces (`SamResources` as root), DTOs/entities, and MicroProfile REST Client annotations. TypeScript types can be generated into `ui/src/main/webui/src/app/model/datamodels.d.ts` via `typescript-generator-maven-plugin` (opt-in with `-Pgenerate-ts`; does not run in normal builds because the plugin cannot handle Java records).
- **core** — Shared business logic and storage SDK integration (CDI beans, no Quarkus runtime dependency).
- **storage** — Parent POM for storage abstraction with three sub-modules: `storage-sdk` (SPI), `storage-local` (filesystem), `storage-s3` (AWS S3).
- **server** — Quarkus runtime: REST resource implementations, Hibernate/Panache entities, Flyway migrations, MapStruct mappers, LangChain4j AI integration, document management, AI-based document classification. Requires PostgreSQL.
- **ui** — Angular frontend served via Quarkus Quinoa. Source lives in `ui/src/main/webui/`.
- **cli** — Quarkus PicoCLI application for importing/exporting sheets, instruments, musicians, and ensembles (see `cli/README.md`). Imports are idempotent (skip-existing) and locally validated. Uses the `api` module as a REST client, authenticating via the `sam-cli` OIDC service account.
- **migration** — Converts a one-time legacy-system export into JSON files the `cli` module can import. See `migration/README.md` for the full data flow and order-of-operations.

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
- **Optimus UI lazy table**: Wire `(onLazyLoad)` only — do **not** also call `load()` from `ngOnInit`. `p-table` with `[lazy]="true"` fires `onLazyLoad` on initialization, so a redundant `ngOnInit` call causes a double request on page load.
- **HTTP subscriptions**: Always pipe through `takeUntilDestroyed(this.destroyRef)` (inject `DestroyRef`) to guard against in-flight responses arriving after component destruction.
- **Document classification**: Two-step AI workflow triggered after upload. `POST /documents/{id}/classify` runs the LangChain4j analyzer and returns `SheetClassification` with detected metadata and pre-matched entity references. `POST /documents/{id}/apply` accepts a reviewed `ClassificationApplyRequest` and creates/resolves musician, instrument, sheet, and instrumentation entities, then links the document as an attachment. For PDFs, text is extracted with `PDFTextStripper` first (cheaper); only scanned/image-only PDFs fall back to GPT-4o vision. Key classes: `server/src/main/java/de/halbmann/sam/classification/`.

## Security

- **OIDC provider**: `sam.oidc.provider` / `SAM_OIDC_PROVIDER` = `pocket-id` (default, also in dev) or `keycloak`; it loads `oidc/<provider>.properties` in `server` and `cli` (`OidcProviderConfigSourceFactory`). Plan: `docs/plans/pocket-id-provider.md`.
- **Dev login**: `docker compose up -d` starts Pocket ID (port 1411) next to Postgres, seeded on first start (groups, `admin1`/`librarian1`/`musician1`, `sam-ui`, `sam-cli`). Users have no passkey: `docker compose run --rm pocket-id-seed login-link admin1` prints a one-time login link. Mailpit (http://localhost:8025) catches Pocket ID's mails (login codes admins send); Pocket ID's settings come from env vars there as in prod (`UI_CONFIG_DISABLED`).
- **Keycloak** (optional): `docker compose -f docker-compose.keycloak.yml up` (port 8180, imports `keycloak/sam-realm.json` on first boot only) plus `-Dsam.oidc.provider=keycloak`. `keycloak/configurator/sam/` is a per-entity copy of the realm that can be re-applied to a running instance (`--profile configure run --rm keycloak-configurator`); keep both in sync by hand, see `keycloak/README.md`.
- **`/oidc-config.json`**, fetched by the Angular app at startup, is `OidcConfigResource` at `/api/oidc-config.json` (`@PermitAll`; `OidcConfigResourceTest` pins the path): issuer and client ID from `OIDC_SERVER_URL` / `OIDC_CLIENT_ID`, plus the provider's scope and role claim. The dev proxy (`ui/src/main/webui/proxy.conf.json`) and Caddy in prod (`docker/Caddyfile`, ADR-0010) rewrite the root path to it, so a provider change needs no UI rebuild.
- **Roles**: `admin`, `music_librarian` (Keycloak realm roles; Pocket ID groups `sam-admin` / `sam-music-librarian`). Ensemble membership lives in the database (`EnsembleMembershipEntity`), not in the token.
- **Auth enforcement**: `@Authenticated` at class level on all `*ResourceImpl` classes (all endpoints require a token). Write methods additionally carry `@RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})`. Role constants are in `server/src/main/java/de/halbmann/sam/security/Roles.java`.
- **Do NOT put `@RolesAllowed` on the `api` module interfaces** — they are also used as REST clients by the `cli` module.
- **Test profile**: `%test.quarkus.oidc.enabled=false` — existing `@QuarkusTest` tests run without auth. Auth-specific tests use `@TestSecurity` from `quarkus-test-security`; add `@OidcSecurity(claims = @Claim(key = "sub", value = "…"))` (`quarkus-test-security-oidc`) when the code needs `CurrentUserService.getUserId()`, which is `null` otherwise.
- **`CurrentUserService`** (`@RequestScoped`, `server/.../security/`): provides `getUserId()`, `getUsername()`, `hasRole()` — inject this instead of `JsonWebToken` directly in business logic.
- **`cli` authentication**: authenticates as the active provider's `sam-cli` client (service account with the `music_librarian` role; Pocket ID: an API permission on `urn:sam:api`) via OIDC client-credentials (`quarkus-rest-client-oidc-filter`), not a user login — see `migration/README.md`'s "Authentication" section for dev/prod config.

## Monitoring

Opt-in Prometheus + Grafana stack — see `monitoring/CLAUDE.md`.

## Database

- PostgreSQL with Flyway migrations in `server/src/main/resources/db/migration/`
- Flyway owns the schema: every entity/column change needs a migration. Hibernate auto-update runs in `%dev` only (so a local DB can hide a missing migration); tests run on the Flyway-only schema and `FlywaySchemaValidationTest` boots with `validate`, as does prod (`%prod...schema-management.strategy=validate` refuses to start on drift).
- Dev services are disabled; dev expects PostgreSQL at `localhost:5432/sam_music`, user `sam` (`docker compose up -d` starts it)
- Extensions required: `pg_trgm`, `fuzzystrmatch`

## Code Style

- Java 25, Palantir Java Format (via Spotless plugin)
- Run `./mvnw spotless:apply` before committing

## Verification Policy

### Backend (Java / Quarkus)

After implementing any backend change:

1. **Write or update tests** — unit tests for new/changed business logic; `@QuarkusTest` integration tests for new endpoints or complex service interactions.
2. **Run the affected tests** and confirm they pass before marking the task done:
   ```bash
   # Single test class (fast)
   rtk ./mvnw test -pl server -am -Dtest=<TestClass> -Dsurefire.failIfNoSpecifiedTests=false

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
   - New features / pages → use `/angular-optimus-browser-verify`
   - Bug fixes → use `/browser-fix-and-recheck`
3. Pay special attention to: Optimus UI overlays, dialogs, tables, form validation, routing transitions, and responsive behavior.
4. Test both the golden path and relevant edge cases (empty state, validation errors, loading state).
5. **Save Playwright screenshots to `target/playwright/`** — `target/` is gitignored, so screenshots never end up in the working tree or a commit.

### When tests are not applicable

If a change is purely structural (rename, move, formatting) or documentation-only and no meaningful test can be written, state this explicitly rather than skipping silently.

### Documentation

`docs/` is organised as small, single-topic, cross-linked pages (arc42-inspired; also
opened as an Obsidian vault). `docs/README.md` is the map of content. Use plain relative
Markdown links between pages — never `[[wikilinks]]` (they don't render on GitHub).
Layout: `docs/architecture/` (context, building-blocks, data-model, deployment,
tech-stack + `concepts/`, `runtime/`, `decisions/`), `docs/features/` (one page per
domain area), `docs/plans/` (task breakdowns for planned roadmap items), `docs/reviews/`
(review backlogs), plus `stakeholders.md` and `roadmap.md` (Now/Next/Later section at the top).

After implementing a feature or bugfix:
1. Update README.md TODOs if the feature is now complete
2. If architecture changed, verify the matching page under `docs/architecture/` (or `docs/architecture/concepts/`) still holds
3. If UI patterns changed, check `ui/src/main/webui/ARCHITECTURE.md`
4. If adding new modules or resources, update `docs/architecture/building-blocks.md` (and `docs/architecture/data-model.md` for new entities)
5. If a `docs/roadmap.md` item shipped, flip its status (`idea`/`planned`/`in progress` → `done`) instead of leaving it stale; for a plan on the Now or Next list, close its task's GitHub issue from the PR (`Closes #N`; the plan row links the issue and is left alone), otherwise flip the task rows in its `docs/plans/` page; delete the plan page once every task is done (conventions: `docs/plans/README.md`)
6. If the change affects who can do what (new/changed `@RolesAllowed`, new endpoint, new share/visibility behavior, new personalised view), check `docs/stakeholders.md` — its use cases and the Section 6 access-control model drift silently and easily (e.g. RBAC and share links stayed marked "planned" long after they shipped)
7. If a new feature or endpoint is user-facing, check the matching page in `docs/features/` for a missing or stale entry (and `docs/features/api-surface.md` for new endpoints)
8. If a significant design decision was made (or reversed), record it as a small ADR in `docs/architecture/decisions/` and link it from the index there
