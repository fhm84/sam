# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SAM (Sheet music Archiving & Management) is a Quarkus-based application for archiving sheet music, managing instrumentations, musicians, and collections for bands/ensembles.

## Build & Run Commands

```bash
# Build all modules
./mvnw package

# Run server in dev mode (live reload, Dev UI at http://localhost:8080/q/dev/)
./mvnw quarkus:dev -pl server

# Run unit tests (all modules)
./mvnw test

# Run a single test class
./mvnw test -pl server -Dtest=SheetImporterTest

# Run integration tests (skipped by default via skipITs=true)
./mvnw verify -pl server -DskipITs=false

# Format code (Palantir Java Format via Spotless)
./mvnw spotless:apply

# Check formatting without fixing
./mvnw spotless:check

# Generate TypeScript types from API entities (opt-in via profile; skipped in normal builds)
./mvnw generate-sources -pl api -Pgenerate-ts

# Build native executable
./mvnw package -Dnative

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
- **Document classification**: Two-step AI workflow triggered after upload. `POST /documents/{id}/classify` runs the LangChain4j analyzer and returns `SheetClassification` with detected metadata and pre-matched entity references. `POST /documents/{id}/apply` accepts a reviewed `ClassificationApplyRequest` and creates/resolves musician, instrument, sheet, and instrumentation entities, then links the document as an attachment. For PDFs, text is extracted with `PDFTextStripper` first (cheaper); only scanned/image-only PDFs fall back to GPT-4o vision. Key classes: `server/src/main/java/de/halbmann/sam/classification/`.

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

- Java 21, Palantir Java Format (via Spotless plugin, version 2.39.0)
- Run `./mvnw spotless:apply` before committing
