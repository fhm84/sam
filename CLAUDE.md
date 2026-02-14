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

# Generate TypeScript types from API entities (runs during api module build)
./mvnw generate-sources -pl api

# Build native executable
./mvnw package -Dnative
```

## Module Architecture

Six Maven modules under parent `de.halbmann:sam`:

- **api** — JAX-RS interfaces (`SamResources` as root), DTOs/entities, and MicroProfile REST Client annotations. Also generates TypeScript types into `ui/src/main/webui/src/app/model/datamodels.d.ts` via `typescript-generator-maven-plugin`.
- **core** — Shared business logic and storage SDK integration (CDI beans, no Quarkus runtime dependency).
- **storage** — Parent POM for storage abstraction with three sub-modules: `storage-sdk` (SPI), `storage-local` (filesystem), `storage-s3` (AWS S3).
- **server** — Quarkus runtime: REST resource implementations, Hibernate/Panache entities, Flyway migrations, MapStruct mappers, LangChain4j AI integration, document management. Requires PostgreSQL.
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

## Database

- PostgreSQL with Flyway migrations in `server/src/main/resources/db/migration/`
- Dev services are disabled; a running PostgreSQL instance is expected (default: `localhost:5432/sam_music`, user `sam`)
- Extensions required: `pg_trgm`, `fuzzystrmatch`

## Code Style

- Java 21, Palantir Java Format (via Spotless plugin, version 2.39.0)
- Run `./mvnw spotless:apply` before committing
