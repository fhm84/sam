<img src="ui/src/main/webui/public/logo.svg" width="96" align="right" alt="SAM logo">

# SAM — Sheet music Archiving & Management

[![PMD](https://github.com/fhm84/sam/actions/workflows/pmd.yml/badge.svg)](https://github.com/fhm84/sam/actions/workflows/pmd.yml)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Quarkus 3.32](https://img.shields.io/badge/Quarkus-3.32-4695EB?logo=quarkus&logoColor=white)](https://quarkus.io)
[![Angular 21](https://img.shields.io/badge/Angular-21-DD0031?logo=angular&logoColor=white)](https://angular.dev)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-required-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org)

A Quarkus-based application for archiving sheet music, managing instrumentations, musicians, and collections for bands and ensembles.

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

**Prerequisites:** Java 21, PostgreSQL (with `pg_trgm` and `fuzzystrmatch` extensions), default connection: `localhost:5432/sam_music`, user `sam`.

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

### CLI
- PicoCLI-based command-line tool for batch importing sheet music data
- Consumes the same REST API interfaces as the server (shared `api` module)

## Architecture

See [docs/architecture.md](docs/architecture.md) for module overview, data model, design decisions, and technology stack.

## Module Structure

```
sam (parent)
 +-- api            Contracts: JAX-RS interfaces, DTOs, enums
 +-- core           Shared business logic (exceptions, utilities)
 +-- storage        Storage abstraction (SPI + local/S3 impls)
 +-- server         Quarkus runtime: REST impls, JPA entities, services
 +-- ui             Angular frontend (served via Quarkus Quinoa)
 +-- cli            PicoCLI batch import tool (REST client)
```

## Roadmap

### Planned Features
- Multitenancy
- Dashboard/statistics (sheets by composer/arranger/genre)
- Auto-convert to MusicXML format (for transposition)
- Event logging (e.g. document downloads)
- Advanced file-upload workflows (linking/metadata in upload dialog)
- System info endpoint (`/api/info`)

### Technical Debt
- Extended revision information (Envers metadata beyond current snapshots)
- Optimize document storage directory structure

