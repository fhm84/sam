# SAM Architecture

**S**heet music **A**rchiving & **M**anagement

## 1. Context & Scope

SAM is a web application for archiving sheet music, managing instrumentations, musicians, and collections for bands and ensembles. It helps answer questions like: *"Do we have all the parts we need to play this piece with our brass ensemble?"*

### System Context

```
                          +----------------+
                          |   Musician /   |
                          |  Band Leader   |
                          +-------+--------+
                                  |
                     browses, uploads, manages
                                  |
                          +-------v--------+
                          |    Angular UI  |
                          | (Quarkus Quinoa)|
                          +-------+--------+
                                  |
                             REST/JSON
                                  |
    +-------------+       +-------v--------+       +----------------+
    |   CLI       +------>|   SAM Server   +------>|  PostgreSQL    |
    | (PicoCLI)   | REST  |  (Quarkus)     |  JDBC |  + pg_trgm     |
    +-------------+       +-------+--------+       |  + fuzzystrmatch|
                                  |                +----------------+
                                  |
                          +-------v--------+
                          | Storage Backend|
                          | (Local / S3)   |
                          +----------------+
                                  |
                          +-------v--------+
                          |   LLM Provider |
                          | (Ollama/OpenAI)|
                          +----------------+
```

**Users** interact via the Angular frontend or the CLI for batch operations. The server is the single backend, persisting to PostgreSQL and storing documents via a pluggable storage layer. AI classification uses LangChain4j with configurable LLM providers.

### Key Quality Goals

| Priority | Goal | Approach |
|----------|------|----------|
| 1 | Findability | Full-text search with fuzzy matching, phonetic search, trigram similarity |
| 2 | Traceability | Full audit trail via Hibernate Envers on all entities |
| 3 | Extensibility | API-first design, pluggable storage, shared interfaces across clients |

---

## 2. Module Overview

SAM is a multi-module Maven project (`de.halbmann:sam`). Each module has a clear responsibility:

```
sam (parent)
 +-- api            Contracts: JAX-RS interfaces, DTOs, enums
 +-- core           Shared business logic (exceptions, utilities)
 +-- storage        Storage abstraction
 |    +-- storage-sdk     SPI (FileSystemProvider, FileSystemWrapper)
 |    +-- storage-local   Local filesystem implementation
 |    +-- storage-s3      AWS S3 implementation
 +-- server         Quarkus runtime: REST impls, JPA entities, services
 +-- ui             Angular frontend (served via Quarkus Quinoa)
 +-- cli            PicoCLI batch import tool (REST client)
```

### Module Dependencies

```
   cli -------> api <------- server
                 ^              |
                 |              +----> core
                 |              +----> storage-sdk
                 |
                ui (consumes generated TypeScript types from api)
```

- **api** defines the contract. No runtime dependencies.
- **server** implements everything. Only module with JPA, Hibernate, Flyway, LangChain4j.
- **cli** consumes the same `api` interfaces as a MicroProfile REST Client.
- **ui** receives auto-generated TypeScript types from `api` DTOs via `typescript-generator-maven-plugin`.

---

## 3. Data Model

### Core Entities

```
sheets ──────────< instrumentations >────────── instruments
  |                     |
  |                     +---< attachments >--- documents
  +---< attachments >--- documents
  |
  +---< collection_sheets >--- sheet_collections
  |                             (FOLDER | SETLIST)
  +---< collection_sheets >--- booklets
```

### Ensemble & Coverage Entities

```
ensembles ──< ensemble_voices ──< voice_options >── instruments
                                   (PRIMARY |
                                    ALTERNATE |
                                    FALLBACK)
```

### Entity Details

| Entity | PK | Key Fields |
|--------|----|------------|
| `sheets` | UUID | title, subtitle, composer, arranger, genre, fingerprint |
| `instrumentations` | UUID | sheet (FK), instrument (FK), partLabel, clef, notationType |
| `instruments` | String | name, displayName, transposition |
| `musicians` | UUID | name, birthYear, deathYear, ipi |
| `documents` | UUID | filename, path, sha256, mimeType, size, refCount |
| `attachments` | UUID | document (FK), type, displayName |
| `sheet_collections` | UUID | name, description, type (FOLDER/SETLIST), date |
| `booklets` | UUID | name, description |
| `ensembles` | UUID | name, description |
| `ensemble_voices` | UUID | ensemble (FK), label, weight, required |
| `voice_options` | UUID | voice (FK), instrument (FK), type, factor |

All entities carry `version` (optimistic locking), `created`, `lastUpdate` timestamps, and have `_AUD` audit mirror tables via Hibernate Envers.

### Search Infrastructure (PostgreSQL)

Sheets have a generated `tsvector` column combining title, subtitle, composer, and arranger with weighted ranks (A/B/C). Three complementary search strategies:

1. **Full-text search** — `tsvector @@ tsquery` with `ts_rank` (weight: 0.70)
2. **Trigram similarity** — `pg_trgm` on title and composer (weight: 0.20 / 0.10)
3. **Phonetic fallback** — `dmetaphone` on composer/arranger names (weight: 0.05)

Denormalized `composer_name`/`arranger_name` columns are kept in sync via database triggers.

---

## 4. Key Design Decisions

### API-First with Shared Interfaces

REST interfaces are defined in the `api` module using JAX-RS + MicroProfile annotations. The same interface is:
- **Implemented** by the server (`*Impl` classes)
- **Consumed** by the CLI via `@RegisterRestClient`
- **Transpiled** to TypeScript types for the Angular frontend

This ensures contract consistency across all consumers.

### Sub-Resource Pattern

Nested resources (instrumentations within sheets, voices within ensembles, etc.) use the JAX-RS sub-resource locator pattern:

```java
// Parent resource interface
@Path("{sheetId}/instrumentations")
InstrumentationsResource instrumentations(@PathParam("sheetId") String sheetId);

// Implementation delegates via ResourceContext
@Override
public InstrumentationsResource instrumentations(String sheetId) {
    return resourceContext.getResource(InstrumentationsResourceImpl.class);
}

// Sub-resource impl receives parent ID via @PathParam field injection
@PathParam("sheetId") String sheetId;
```

### Content-Addressed Document Storage

Documents are stored by SHA-256 hash. Uploading the same file twice does not create a duplicate — instead, the existing document's reference count is incremented. The `attachments` table links documents to sheets/instrumentations with typed metadata.

The storage layer uses an SPI (`FileSystemProvider` / `FileSystemWrapper`) so backends can be swapped between local filesystem and S3 without changing business logic.

### Fingerprinting & Deduplication

Each sheet music entry gets a deterministic fingerprint computed from its metadata (title, composer, etc.) at persist time via `@PrePersist`. A unique constraint on the fingerprint column prevents duplicate entries.

### Ensemble Coverage Evaluation

Coverage scoring uses a weighted, multi-factor approach:

1. For each **ensemble voice**, find the best matching instrumentation via its voice options
2. Each match is scored by: `instrumentIdMatch * transpositionFactor * clefFactor * notationTypeFactor`
3. Overall coverage: `Sum(voiceWeight * optionFactor * matchScore) / Sum(voiceWeight)`
4. Status thresholds: >= 0.9 = COMPLETE, >= 0.5 = PLAYABLE, else INCOMPLETE
5. Missing required voices always result in INCOMPLETE regardless of score

### AI-Powered Sheet Analysis

LangChain4j with `@RegisterAiService` provides vision-based analysis of uploaded sheet music images/PDFs. The system:
1. Converts PDFs to images (extracting header/footer regions)
2. Sends the image to an LLM (Ollama, OpenAI, or Vertex AI Gemini — configurable)
3. Extracts structured metadata (title, composer, instrument, clef, etc.)

This is used to pre-populate fields during the archiving workflow.

### Audit Trail

Every entity is annotated with `@Audited` (Hibernate Envers). Each table has a corresponding `_AUD` table using `ValidityAuditStrategy` (tracks both revision start and end). This provides a complete history of all changes.

---

## 5. Technology Stack

| Concern | Technology | Version |
|---------|-----------|---------|
| Runtime | Quarkus | 3.31.3 |
| Language | Java | 21 |
| ORM | Hibernate ORM + Panache | (via Quarkus BOM) |
| Audit | Hibernate Envers | (via Quarkus BOM) |
| Database | PostgreSQL | (pg_trgm, fuzzystrmatch) |
| Migrations | Flyway | (via Quarkus BOM) |
| REST | JAX-RS (RESTEasy) | (via Quarkus BOM) |
| Serialization | JSON-B | (via Quarkus BOM) |
| AI | LangChain4j (Quarkus ext.) | 1.7.1 |
| Frontend | Angular | (via Quarkus Quinoa) |
| CLI | PicoCLI (Quarkus ext.) | (via Quarkus BOM) |
| Code Gen | Lombok, MapStruct | 1.18.42, 1.6.3 |
| TS Gen | typescript-generator-maven-plugin | (in api module) |
| Formatting | Palantir Java Format (Spotless) | (plugin 3.2.1) |
| Container | Jib | (via Quarkus ext.) |
