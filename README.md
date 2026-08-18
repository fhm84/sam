<img src="ui/src/main/webui/public/logo.svg" width="96" align="right" alt="SAM logo">

# SAM — Sheet music Archiving & Management

[![PMD](https://github.com/fhm84/sam/actions/workflows/pmd.yml/badge.svg)](https://github.com/fhm84/sam/actions/workflows/pmd.yml)
[![Java 25](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/25/)
[![Quarkus 3.38](https://img.shields.io/badge/Quarkus-3.38-4695EB?logo=quarkus&logoColor=white)](https://quarkus.io)
[![Angular 21](https://img.shields.io/badge/Angular-21-DD0031?logo=angular&logoColor=white)](https://angular.dev)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-required-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org)

A Quarkus-based application for archiving sheet music, managing instrumentations, musicians, and collections for bands and ensembles.

> **Copyright and licensing**
>
> SAM is a tool for managing sheet music archives. It does not grant
> permission to scan, reproduce, store, distribute, or share copyrighted
> sheet music. Users and operators are responsible for obtaining all
> required permissions and licenses and for respecting applicable copyright
> law. This applies in particular to scanned PDFs and other digital copies.
>
> Only upload or share material for which the required rights or permissions
> exist, such as public-domain material, appropriately licensed material,
> or material covered by a specific permission.

## Quick Start

```bash
# Build all modules
./mvnw package

# Run server in dev mode (Dev UI at http://localhost:8080/q/dev/)
./mvnw quarkus:dev -pl server

# Run tests
./mvnw test

# Format code
./mvnw spotless:apply
```

**Prerequisites:** Java 25, PostgreSQL (with `pg_trgm` and `fuzzystrmatch` extensions), default connection: `localhost:5432/sam_music`, user `sam`.

## Features

### Sheet Music Management
- Full CRUD for sheet music entries (title, subtitle, composer, arranger, publisher, genre, difficulty level, copyright, ISWC, GEMA work number, etc.)
- Paginated search with full-text search (PostgreSQL `tsvector`), trigram fuzzy matching (`pg_trgm`), and phonetic search (`dmetaphone`)
- Content-based fingerprinting for deduplication of sheet music

### Instrumentations
- Per-sheet instrumentation management (sub-resource of sheets)
- Each instrumentation links to a canonical instrument with part label, clef, notation type
- Bulk import of instrumentations
- Document attachments per instrumentation

### Instruments
- Canonical instrument registry with transposition metadata (C, Bb, Eb, F, etc.)
- Paginated, filterable CRUD

### Musicians
- Composer/arranger registry with birth/death years and IPI numbers
- Paginated, filterable CRUD

### Collections & Booklets
- **Sheet Collections** — group sheets into folders or setlists, with optional date (e.g. for gig programs)
- **Booklets** — named groupings of sheets (e.g. "27 Weihnachtslieder", "Gotteslob")
- Both support adding/removing/reordering sheets with per-collection identifiers

### Ensembles & Coverage Evaluation
- **Ensemble definitions** — define named ensembles with weighted, prioritized voice requirements
- **Ensemble voices** — each voice has a label, weight, required flag, and multiple instrument options (primary/alternate/fallback with scoring factors)
- **Coverage evaluation** — score-based matching of a sheet's instrumentations against an ensemble definition, producing per-voice breakdown with explanations and an overall coverage status (Complete / Playable / Incomplete)

### Document Management
- File upload/download with content-addressed storage (SHA-256 deduplication, reference counting)
- Pluggable storage backends: local filesystem, AWS S3
- Attachment system linking documents to sheets and instrumentations (typed: full score, part, cover, lyrics, MIDI, audio, etc.)

### AI-Powered Classification
- LangChain4j integration for automated sheet music analysis
- PDF-to-image conversion with intelligent cropping (header/footer extraction)
- Vision-based extraction of metadata (title, composer, instrument, etc.) from scanned sheet music images

### Audit Trail
- Full entity history via Hibernate Envers (every table has an `_AUD` mirror)
- Validity audit strategy with revision start/end tracking
- Event log for read-side events (downloads, exports, AI classification) not captured by Envers: records `userId`, `username`, `eventType`, `entityType`, `entityId`, and a JSONB `metadata` payload; queryable via `GET /api/event-logs` with filtering by event type (multi-select), user, and entity type; pre-wired for share-link access via `shareTokenId`

### CLI
- PicoCLI-based command-line tool for batch importing sheet music data
- Consumes the same REST API interfaces as the server (shared `api` module)

## Architecture

See the [documentation map](docs/README.md) for the full docs — [module overview](docs/architecture/building-blocks.md), [data model](docs/architecture/data-model.md), [design decisions](docs/architecture/decisions/README.md), and [technology stack](docs/architecture/tech-stack.md).

## Module Structure

```
sam (parent)
 +-- api            Contracts: JAX-RS interfaces, DTOs, enums
 +-- core           Shared business logic (exceptions, utilities)
 +-- storage        Storage abstraction (SPI + local/S3 impls)
 +-- server         Quarkus runtime: REST impls, JPA entities, services
 +-- ui             Angular frontend (served via Quarkus Quinoa)
 +-- cli            PicoCLI batch import/export tool (REST client, see cli/README.md)
 +-- migration      Legacy-data conversion for the cli import tool (see migration/README.md)
```

## Configuration

### Environment Variables (Production)

These are read at startup via MicroProfile Config placeholders in `application.properties`.

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/sam_music` | JDBC connection URL |
| `DB_USER` | `sam` | Database username |
| `DB_PASS` | `s4mP@ssword` | Database password |
| `OIDC_SERVER_URL` | — | Keycloak realm base URL, e.g. `https://auth.example.com/realms/sam` |
| `OIDC_CLIENT_ID` | `sam-ui` | OIDC client ID registered in Keycloak |
| `KEYCLOAK_ADMIN_URL` | — | Keycloak server URL for the admin REST API |
| `KEYCLOAK_REALM` | `sam` | Keycloak realm used for admin user lookup |
| `KEYCLOAK_BACKEND_CLIENT_ID` | `sam-backend` | Service account client ID (needs `view-users` role) |
| `KEYCLOAK_BACKEND_CLIENT_SECRET` | — | Service account client secret |
| `SAM_CLI_CLIENT_ID` | `sam-cli` | `cli` module's service account client ID (needs `music_librarian` role), read together with `OIDC_SERVER_URL` above |
| `SAM_CLI_CLIENT_SECRET` | `sam-cli-secret` (dev-only) | `cli` module's service account client secret — must be overridden for real deployments |

### Application Config (`application.properties`)

SAM-specific config keys are defined as constants in `server/.../EnvConsts.java`.

| Key | Default | Description |
|-----|---------|-------------|
| `sam.filesystem.base.path` | — | **Required.** Storage root for uploaded files. See [Storage Backends](#storage-backends). |
| `sam.files.types` | *(allow all)* | Optional file-type filter applied on upload. Whitelist: `pdf\|xml`; blacklist (prefix `^`): `^exe\|bat`. |
| `sam.coverage.base-score` | `0.7` | Minimum score (0–1) awarded to an ensemble voice with any positive instrumentation match. |
| `sam.classification.agentic` | `false` | Enable agentic LLM entity resolution pass after AI classification. |
| `sam.admin.keycloak.realm` | — | Keycloak realm queried by the admin user-search endpoint (set per profile). |

### Storage Backends

`sam.filesystem.base.path` selects the backend automatically from its URI scheme:

| Value | Backend | Notes |
|-------|---------|-------|
| `/data/sam-docs` (bare path) | Local filesystem | Equivalent to `file:///data/sam-docs` |
| `s3://my-bucket/sam-docs` | AWS S3 | Requires AWS SDK credentials (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION` or `quarkus.s3.*` properties) |

### LLM Providers

Set `quarkus.langchain4j.chat-model.provider` and enable exactly one provider:

| Provider value | Required additional config |
|----------------|---------------------------|
| `openai` (default) | `quarkus.langchain4j.openai.api-key=<key>`; model defaults to `gpt-4o` |
| `ollama` | `quarkus.langchain4j.ollama.base-url=http://localhost:11434`; `quarkus.langchain4j.ollama.chat-model.model-id=llava` |
| `vertexai` (Gemini) | `quarkus.langchain4j.vertexai.gemini.project-id=<id>` and `.location=<region>`; model: `gemini-2.0-flash` |

## Copyright and Licensing

SAM is software for cataloguing and managing sheet music. It does not
provide any rights to the musical works, sheet music editions, lyrics,
recordings, or other files managed with the application.

The operator of a SAM instance is responsible for:

- verifying the copyright and licensing status of each uploaded work;
- obtaining permission to scan or otherwise digitise sheet music;
- restricting access and downloads where required;
- ensuring that shared collections and public links comply with the
  applicable licence terms;
- deleting material when the applicable permission or licence expires.

Do not upload, distribute, or share copyrighted sheet music unless the
required permission or licence has been obtained.

The software licence of SAM is separate from the rights in any content
stored in a SAM instance.

For Germany, see for example:

- [MIZ: Noten vervielfältigen, bearbeiten, veröffentlichen und verbreiten](https://miz.org/de/tutorials/noten-vervielfaeltigen-bearbeiten-veroeffentlichen-und-verbreiten)
- [VG Musikedition: Vervielfältigungen](https://vg-musikedition.de/nutzer/vervielfaeltigungen)
- [frag-amu.de: Digitale Noten](https://frag-amu.de/digitale-noten/)

## Contributing

### Commit Messages

Commits follow [Conventional Commits](https://www.conventionalcommits.org/): `<type>(<scope>): <subject>`.

- **Types:** `build`, `chore`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `revert`, `style`, `test`
- **Scope:** optional, usually a module (`ui`, `core`, `api`, `server`, `cli`, `storage`) or area (`docs`, `arch`); comma-separated for multiple (`test(core,api): ...`)
- Examples: `fix(ui): pin transitive deps to patch Dependabot security alerts`, `test(core,api): add plain JUnit tests for pure business logic`

Enable local validation once per clone:

```bash
git config core.hooksPath .githooks
```

Pull requests are also checked by the `commit-lint` CI job (`.github/workflows/ci.yml`); both call the shared `scripts/check-commit-msg.sh`.

## Roadmap

### Planned Features
- Multitenancy
- Dashboard/statistics (sheets by composer/arranger/genre)
- Auto-convert to MusicXML format (for transposition)
- Advanced file-upload workflows (linking/metadata in upload dialog)

### Technical Debt
- Extended revision information (Envers metadata beyond current snapshots)
- Optimize document storage directory structure

