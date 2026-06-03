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
| `instruments` | String | name, displayName, transposition, **family**, **defaultClef**, **catalogSection**, **catalogPosition** |
| `instrument_aliases` | (instrument_id, alias_order) | alias strings for OCR matching; ordered list per instrument |
| `musicians` | UUID | name, birthYear, deathYear, ipi, **userId** (OIDC subject — null for external/historical musicians), **email**, **mobile**, **notes**, **status**, **role**, **lastInviteSentAt** |
| `musician_instruments` | UUID | musician (FK), instrument (FK), isPrimary — instrument assignments driving "My Parts" personalisation |
| `documents` | UUID | filename, path, sha256, mimeType, size, refCount |
| `attachments` | UUID | document (FK), type, displayName |
| `sheet_collections` | UUID | name, description, type (FOLDER/SETLIST), date, **visibility**, **coverColor**, **coverImageId** (FK → documents) |
| `booklets` | UUID | name, description |
| `ensembles` | UUID | name, description |
| `ensemble_voices` | UUID | ensemble (FK), label, weight, required |
| `voice_options` | UUID | voice (FK), instrument (FK), type, factor |
| `ensemble_memberships` | UUID | musician (FK), ensemble (FK), voice (FK, nullable), instrument (FK, nullable), conductor (bool) — one row per musician per voice per ensemble |
| `shares` | UUID | creatorUserId, resourceType, resourceId, expiresAt, revokedAt, createdAt |
| `event_log` | UUID | occurredAt, userId, username, eventType, entityType, entityId, metadata (JSONB), shareTokenId |

All domain entities carry `version` (optimistic locking), `created`, `lastUpdate` timestamps, and have `_AUD` audit mirror tables via Hibernate Envers. `shares` and `event_log` are append-only and are not audited.

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

### Ensembles & Coverage Evaluation

SAM models ensembles as **Besetzungsrahmen** for amateur bands – focusing on playability rather than perfect theoretical coverage.

- **Ensemble definitions**
  Define named ensembles as collections of instrument groups (e.g. Trumpets, Saxophones, Tenorhorns), independent of piece-specific musical roles.

- **Ensemble voices**
  Each voice represents an instrument group and defines:
    - `required` — whether the group is mandatory for playability
    - `minCount` — minimum number of parts required to be playable
    - `targetCount` — ideal number of parts for good balance
    - `weight` — musical importance of the group
    - one or more `VoiceOption` entries (PRIMARY / ALTERNATE / FALLBACK), each specifying an instrument and a `factor` (1.0 = ideal, < 1.0 = substitute quality)
    - **A voice without options cannot be matched by any instrumentation**

- **Instrument matching (`MatchingService`)**
  A voice option matches an instrumentation only when the instrument IDs are identical (exact match). No automatic transposition or alias fallback — substitution must be modelled explicitly via ALTERNATE/FALLBACK options.
  Two secondary modifiers can reduce the score below 1.0:
    - *Clef factor* (0.7 for non-transposing instruments when a specific clef is set)
    - *Notation-type factor* (0.8 for percussion notation, 0.7 for tablature/graphic)
    - Scores below 0.3 are treated as no match.

- **Coverage evaluation**
  Voices are processed in priority order — required voices first, then by weight descending — so that the most important seats get first pick of available instrumentations. Each sheet instrumentation is claimed by at most one voice (greedy allocation).

  Per voice:
  ```
  effectiveCount = Σ (matchScore × option.factor)   for each claimed instrumentation
  normalized     = min(effectiveCount / targetCount, 1.0)
  countScore     = baseScore + (1 − baseScore) × normalized
  ```
  where `baseScore` (default **0.7**, configurable via `sam.coverage.base-score`) ensures any positive match immediately scores at least 70% of the voice’s contribution.

  Overall:
  ```
  coverageScore = Σ(countScore × voice.weight) / Σ(voice.weight)
  ```

  A required voice with `effectiveCount < minCount` marks the piece as not playable and contributes 0 to the score regardless of partial matches.

- **Status classification**

  | Status | Condition |
  |--------|-----------|
  | `INCOMPLETE` | One or more required voices are missing |
  | `PLAYABLE` | All required voices covered; `coverageScore < 0.85` |
  | `COMPLETE` | All required voices covered; `coverageScore ≥ 0.85` |

- **Coverage snapshots**
  Live evaluation is triggered on demand. Results are stored as `CoverageSnapshot` records (one per ensemble × sheet pair, upserted) so the sheets list can display coverage badges without per-request evaluation. Snapshots must be manually recomputed via `POST /api/ensembles/{id}/coverage/compute` — there is no automatic invalidation.

- **Coverage details**
  Evaluation results include a per-voice breakdown with effective part count, normalised score, and a human-readable explanation of which instrumentations were matched and via which option.


### AI-Powered Sheet Classification

LangChain4j with `@RegisterAiService` provides AI-based classification of uploaded sheet music. The workflow is a two-step process:

**Step 1 — Classify (`POST /documents/{id}/classify`)**

1. **Text extraction (PDFBox)** — For native PDFs, text is extracted first (fast, free). If fewer than 50 printable characters are found, the document is treated as scanned/image-only.
2. **Vision fallback** — Scanned PDFs and images are converted to PNG and sent to an LLM (Ollama, OpenAI, or Vertex AI Gemini — configurable) using the vision API.
3. **Structured metadata extraction** — A `SheetAnalyzerResult` is returned containing title, composer, arranger, genre, year, instrumentation details, etc.
4. **Entity pre-matching** — Composer and arranger names are matched against existing musicians; existing sheets are matched by exact title; instruments are matched via pg_trgm trigram similarity (threshold 0.3, up to 5 candidates with scores).
5. **Pre-filled suggestion (Option A)** — A `ClassificationApplyRequest` is built automatically from the AI result and best-match candidates. This is returned to the frontend for user review.
6. **Agentic resolution (Option B, opt-in)** — When `sam.classification.agentic=true`, a second AI pass using `ClassificationAgent` autonomously resolves entity references via tool calls (`searchSheets`, `searchMusicians`, `searchInstruments`) before returning the suggestion. Falls back to Option A on error.

**Step 2 — Apply (`POST /documents/{id}/apply`)**

The reviewed `ClassificationApplyRequest` is submitted. The service:
- Resolves or creates the sheet, composer, arranger, and instrument entities
- Creates an `InstrumentationEntity` if instrument information was provided
- Attaches the document as an `AttachmentEntity` to the instrumentation (or directly to the sheet)

This is used to pre-populate and confirm the archiving workflow with minimal manual input.

### Authentication & Authorization

Quarkus OIDC with self-hosted **Keycloak 26** (`docker-compose.keycloak.yml`; realm export at `keycloak/sam-realm.json`).

| Concern | Mechanism |
|---|---|
| Identity | `Musician.userId` = OIDC `sub` claim. No separate User entity — a musician either has a login or doesn't. |
| Ensemble access | Keycloak group `ensemble:{UUID}` in the JWT `groups` claim; read by `CurrentUserService.getAccessibleEnsembleIds()` |
| Roles | Keycloak realm roles: `music_librarian` (full archive write access), `admin` (system config) |
| Public access | `/public/share/{token}` endpoints bypass `@Authenticated`; token is validated manually in `PublicShareResourceImpl` |

`CurrentUserService` (`@RequestScoped`) is the single injection point for identity in business logic — wraps JWT parsing, role checks, and ensemble-group resolution. Inject this instead of `JsonWebToken` directly.

`MyPartsService` extends this pattern for the personalized sheet view: it calls `currentUserService.getUserId()` to look up the linked `MusicianEntity`, collects all instrument IDs from the musician's `EnsembleMembershipEntity` records, and returns only sheets that contain at least one matching instrumentation. Conductor memberships (null instrument) are skipped automatically.

All `*ResourceImpl` classes carry `@Authenticated` at class level. Write methods additionally carry `@RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})`. API interface definitions in the `api` module remain role-free so they can be used as REST clients in the `cli` module.

#### Musician–User linking (admin UI)

Admins can link a `Musician` record to an authenticated account via the musician edit form. The link is the `userId` field (OIDC subject claim) on `MusicianEntity`.

Dedicated endpoints (admin-only):
- `PUT /api/musicians/{id}/user/{userId}` — sets the link
- `DELETE /api/musicians/{id}/user` — clears the link

The general `PUT /api/musicians/{id}` (used by the form save) intentionally ignores `userId` via `@Mapping(target = "userId", ignore = true)` in `MusicianMapper`, so a librarian updating a musician's name can never accidentally clear an existing link.

User lookup for the admin search autocomplete is backed by the **Keycloak Admin REST API** via `quarkus-keycloak-admin-rest-client`. The `AdminUsersResource` (`GET /api/admin/users?search=`, `GET /api/admin/users/{id}`) proxies user searches to Keycloak and is restricted to the `admin` role. In dev, the admin client authenticates against the `master` realm using the bootstrap admin credentials (`admin`/`admin`). In production, configure `KEYCLOAK_ADMIN_URL`, `KEYCLOAK_ADMIN_USER`, `KEYCLOAK_ADMIN_PASSWORD`, and `KEYCLOAK_REALM` environment variables.

Test profile: `%test.quarkus.oidc.enabled=false`. Auth-specific tests use `@TestSecurity` from `quarkus-test-security`.

### Audit Trail

Every domain entity is annotated with `@Audited` (Hibernate Envers). Each table has a corresponding `_AUD` table using `ValidityAuditStrategy` (tracks both revision start and end). This provides a complete history of all data mutations.

Read events (document downloads, exports, AI classification, share-link access) are tracked separately in the `event_log` table via `EventLogService` — Envers only covers writes.

---

## 5. Technology Stack

| Concern | Technology | Version |
|---------|-----------|---------|
| Runtime | Quarkus | 3.33.1.1 (LTS) |
| Language | Java | 21 |
| ORM | Hibernate ORM + Panache | (via Quarkus BOM) |
| Audit | Hibernate Envers | (via Quarkus BOM) |
| Database | PostgreSQL | (pg_trgm, fuzzystrmatch) |
| Migrations | Flyway | (via Quarkus BOM) |
| REST | JAX-RS (RESTEasy) | (via Quarkus BOM) |
| Serialization | JSON-B | (via Quarkus BOM) |
| AI | LangChain4j (Quarkus ext.) | 1.7.1 |
| Frontend | Angular | 21.2.8 |
| Frontend UI | PrimeNG (Aura preset) | 21.1.1 |
| CLI | PicoCLI (Quarkus ext.) | (via Quarkus BOM) |
| Code Gen | Lombok, MapStruct | 1.18.44, 1.6.3 |
| TS Gen | typescript-generator-maven-plugin | (in api module) |
| Formatting | Palantir Java Format (Spotless) | (plugin 3.3.0) |
| Container | Jib | (via Quarkus ext.) |
