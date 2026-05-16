# SAM – Feature Reference

**S**heet music **A**rchiving & **M**anagement

This document describes the **current feature set** of SAM as a living reference.
For architectural decisions see `architecture.md`, for planned work see `roadmap.md`.

---

## Table of Contents

1. [Sheet Music](#1-sheet-music)
2. [Instrumentations](#2-instrumentations)
3. [Documents & Attachments](#3-documents--attachments)
4. [AI Classification](#4-ai-classification)
5. [AI Data Enrichment](#5-ai-data-enrichment)
6. [Musicians](#6-musicians)
7. [Instruments](#7-instruments)
8. [Collections & Setlists](#8-collections--setlists)
9. [Ensembles & Coverage](#9-ensembles--coverage)
10. [Shares & Public Access](#10-shares--public-access)
11. [Event Log](#11-event-log)
12. [Search & Discovery](#12-search--discovery)
13. [UI & Personalisation](#13-ui--personalisation)

---

## 1. Sheet Music

The central entity. Each sheet represents one piece of music in the archive.

### Metadata fields

| Field | Type | Notes |
|-------|------|-------|
| Title | String (required) | |
| Subtitle | String | |
| Composer | Musician reference | Linked to the musicians catalogue |
| Arranger | Musician reference | Linked to the musicians catalogue |
| Original by | String | Original band / artist for covers/arrangements |
| Genre | Enum (required) | Structural form: March, Suite, Overture, etc. — see [Genre enum](#genre) |
| Style | Enum | Stylistic character: Classical, Swing, Pop, etc. — see [Style enum](#style) |
| Difficulty level | 1–6 scale | Wind-band grading: 1 = very easy … 6 = very difficult |
| Duration | hh:mm:ss | Approximate playtime |
| Year of composition | Integer | |
| Publisher | String | |
| Publisher IPI | String | Publisher Interested Parties Information code |
| Edition | String | e.g. "Revised 2010", "Score & Parts" |
| Copyright | String | |
| ISWC | String | International Standard Musical Work Code |
| GEMA work number | String | German GEMA identifier |
| Additional notes | Free text | General remarks, performance notes |
| Tags | Set\<String\> | Free-form labels (e.g. "christmas", "outdoor", "opening") |
| Rating | 1–5 | User rating |
| Favorite | Boolean | Quick-access flag |

### Actions

- **Create / edit / delete** via a dedicated full-page form.
- **Tags** can be added and removed individually without editing the full sheet.
- **Favorite** can be toggled directly from the list and detail views.
- **Fingerprint-based deduplication** — creating a sheet with identical core metadata is rejected at the database level.

### Enums

#### Genre
Describes the structural/formal category of the piece.

`MARCH` · `MARCHING_SHOW` · `CONCERT_WORK` · `OVERTURE` · `SUITE` · `SYMPHONY` ·
`FANTASY` · `VARIATIONS` · `DANCE` · `WALTZ` · `POLKA` · `FOLK_SONG` ·
`HYMN_CHORALE` · `FILM_MUSIC` · `SHOW_MUSIC` · `POP_ROCK` · `JAZZ` ·
`LATIN` · `CHRISTMAS` · `SACRED` · `SOLO_WITH_BAND`

#### Style
Describes the aesthetic or period character (optional, complementary to genre).

`CLASSICAL` · `ROMANTIC` · `MODERN` · `CONTEMPORARY` · `POP` · `ROCK` ·
`FUNK` · `SWING` · `LATIN` · `TRADITIONAL` · `FOLKLORISTIC` · `EXPERIMENTAL`

#### Difficulty level

| Grade | Label |
|-------|-------|
| 1 | Very Easy |
| 2 | Easy |
| 3 | Medium |
| 4 | Advanced |
| 5 | Difficult |
| 6 | Very Difficult |

---

## 2. Instrumentations

Each sheet can have multiple instrumentations — one per instrument part.

### Metadata fields

| Field | Type | Notes |
|-------|------|-------|
| Instrument | Instrument reference | From the instruments catalogue |
| Part label | String | Distinguishes parts, e.g. "1st Trumpet", "Solo", "2. Bass" |
| Clef | Enum | `TREBLE` · `ALTO` · `TENOR` · `BASS` |
| Notation type | Enum | `STANDARD` · `TABLATURE` · `PERCUSSION` · `LEAD_SHEET` · `GRAPHIC` |
| Notes | Free text | Part-specific performance notes |
| Archive location | String | Physical storage location of the printed copy (e.g. "Cabinet A / Shelf 3 / Folder 12") |
| Physical condition | Enum | `GOOD` · `WORN` · `DAMAGED` · `LOST` — state of the printed copy |

### Actions

- **Create / edit / delete** via a dialog within the sheet detail view.
- **Bulk creation** — multiple instrumentations can be added in a single request.
- Each instrumentation has its own **document attachments** (individual part files).

---

## 3. Documents & Attachments

SAM manages physical files (PDFs, audio, images, etc.) separately from metadata.

### Upload

Files are uploaded to a **staging area** (unlinked pool) or directly to a sheet or instrumentation. Supported via drag-and-drop or file picker.

### Attachment types

| Type | Description |
|------|-------------|
| `FULL_SCORE` | Complete conductor's score |
| `PART` | Individual instrument part |
| `COVER` | Title page / decorative cover |
| `LYRICS` | Text-only lyrics document |
| `MIDI` | MIDI playback file |
| `AUDIO` | MP3 / WAV recording |
| `ANNOTATIONS` | Score with markings, fingerings |
| `IMAGE` | Scanned JPG / PNG |
| `ANALYSIS` | Harmonic or structural analysis |
| `TRANSCRIPTION` | Manually transcribed version |
| `EXTERNAL_LINK` | URL to an external resource |
| `MUSIC_XML` | MusicXML / MXL exchange format |
| `OTHER` / `UNSPECIFIED` | Catch-all |

### Document features

- **Content-addressed storage** — files are identified by SHA-256 checksum. Uploading the same file twice increments a reference count instead of duplicating storage.
- **Pluggable storage backend** — local filesystem or AWS S3 (configured via `quarkus.langchain4j.*`).
- **ETag-based HTTP caching** on download.
- **Linking & relinking** — a document in the unlinked pool can be assigned to a sheet or instrumentation at any time. Existing links can be edited (target sheet, target instrumentation, attachment type).
- **Batch download** — select multiple documents for download as a ZIP archive or as a **merged PDF** (when all selected files are PDFs).
- **Unlinked pool** (`/uploads`) — staging area showing all documents not yet assigned, with classify and assign actions.

---

## 4. AI Classification

Two-step workflow that reads a document and pre-populates the archiving form.

### Step 1 — Classify (`POST /documents/{id}/classify`)

1. **Text extraction** (PDFBox) — if ≥ 50 printable characters are found in the PDF, the text path is used (fast, no vision API calls).
2. **Vision fallback** — scanned PDFs and images are converted to PNG and analysed by the configured LLM (Ollama / OpenAI / Vertex AI Gemini).
3. **Metadata extraction** — the AI returns: title, subtitle, publisher, composer, arranger, genre, year, edition, ISWC, instrument name, part label, clef, notation type.
4. **Entity pre-matching** against existing data:
   - Composer / arranger — trigram similarity (pg_trgm)
   - Sheet — exact title match
   - Instruments — trigram similarity (threshold 0.3, up to 5 ranked candidates)
5. A `SheetClassification` is returned containing the raw AI result, pre-matched entity IDs, and a pre-filled `ClassificationApplyRequest` ready for user review.

**Agentic mode** (opt-in via `sam.classification.agentic=true`) — a second AI pass using tool calls (`searchSheets`, `searchMusicians`, `searchInstruments`) resolves entity references autonomously before building the suggestion. Falls back to the standard suggestion on error.

### Step 2 — Apply (`POST /documents/{id}/apply`)

The user reviews and adjusts the pre-filled form, then submits a `ClassificationApplyRequest`. The server:

- Resolves or **creates** the sheet, composer, arranger, and instrument as needed.
- Creates a new `Instrumentation` if instrument details are provided.
- Attaches the document to the instrumentation (or directly to the sheet).

### Classification dialog (UI)

- Split-panel view: document preview on the left, review form on the right.
- Mode toggles for each entity: *use existing* · *create new* · *none*.
- Instrument candidates shown as a scored dropdown.
- Error state with retry option.

---

## 5. AI Data Enrichment

Suggests missing or complementary metadata for an **existing** sheet, based on its current metadata (no document required).

### Endpoint

`POST /sheets/{id}/enrich` — analyses the sheet's title, composer, arranger, genre, and other known fields, then returns:

| Suggestion | Condition |
|-----------|-----------|
| Tags | Always suggested (new, not already in the tag set) |
| Style | Only when style is not yet set |
| Difficulty level | Only when difficulty is not yet set |
| Year of composition | Only when year is not yet set |
| Additional notes | Only when notes are empty |

### Enrichment dialog (UI)

- Opened via the **"Enrich with AI"** (sparkles) button in the sheet detail header.
- Loading spinner while the AI runs.
- Per-suggestion checkboxes — pre-checked, user can deselect individually.
- Tags shown as chip-style checkboxes.
- "Apply selected" calls `PUT /sheets/{id}` and reloads the sheet.
- "No suggestions" state when metadata is already complete.

---

## 6. Musicians

A shared reference catalogue of composers, arrangers, and ensemble members.

### Metadata fields

| Field | Type | Notes |
|-------|------|-------|
| Name | String (required) | |
| IPI | String | Interested Party Information code (9-digit rights holder ID) |
| Birth year | Integer | |
| Death year | Integer | |
| User ID | String | OIDC subject claim — links this musician to a system user account. Null for external/historical musicians with no login. |

### Actions

- **Create / edit / delete** via a dialog.
- Paginated list with search by name.
- Referenced from sheets as composer / arranger.
- Can be assigned to ensembles as members (see Section 9).

---

## 7. Instruments

A canonical instrument catalogue used across instrumentations and ensemble voice definitions.

### Metadata fields

| Field | Type | Notes |
|-------|------|-------|
| ID | String (slug) | Unique, immutable key, e.g. `trumpet-bb` |
| Name | String (required) | Canonical name, e.g. "Bb Trumpet" |
| Display name | String | Short name shown in UI |
| Transposition | Enum | Concert pitch key: `C` · `Bb` · `Eb` · `F` · `Ab` · `D` · `A` · `G` |

### Actions

- **Create / edit / delete** via a dialog.
- Paginated list with search by name and filter by transposition.

---

## 8. Collections & Setlists

Group sheets into named collections for organisation and concert planning.

### Types

| Type | Description |
|------|-------------|
| `FOLDER` | Static grouping of related pieces (e.g. "Christmas repertoire") |
| `SETLIST` | Ordered program for a specific concert or event, with an optional date |

### Metadata fields

| Field | Type | Notes |
|-------|------|-------|
| Name | String (required) | |
| Description | String | |
| Type | `FOLDER` or `SETLIST` | |
| Date | Date | For setlists (performance date) |

### Collection sheets (membership)

Each entry in a collection links to a sheet and carries:
- **Identifier** — position or label within the collection (e.g. `1`, `A1`, `Intro`)

### Actions

- **Create / edit / delete** collections and individual sheet memberships.
- Paginated sheet list within each collection.
- Sheet detail preview accessible from within the collection.
- **Export formats:** JSON, CSV, and ZIP (metadata + attached documents)
- **Table of contents:** Generate a PDF TOC from a collection/setlist (via `CollectionTocService`)

---

## 9. Ensembles & Coverage

Model the target instrumentation of a band, then evaluate how well a piece is covered.

### Ensemble definition

An ensemble represents a band configuration (e.g. "Brass Quintet", "20-piece Blaskapelle").

#### Voices

Each ensemble has a list of **voices** (instrument groups):

| Field | Description |
|-------|-------------|
| Label | Display name (e.g. "1. Trumpet", "Tenorhorn", "Tuba") |
| Required | Whether the voice must be covered for the piece to be playable |
| Min count | Minimum number of parts for playability |
| Target count | Ideal number of parts |
| Weight | Musical importance for scoring (higher = more impact on overall score) |

#### Voice options

Each voice has one or more instrument options modelling substitution:

| Type | Description |
|------|-------------|
| `PRIMARY` | Ideal instrument for this voice |
| `ALTERNATE` | Accepted substitute |
| `FALLBACK` | Last resort |

Each option has a **factor** (0.0–1.0): 1.0 = ideal, lower values downweight substitute quality.

### Coverage evaluation

`GET /sheets/{id}/coverage?ensemble={ensembleId}` evaluates the piece on-demand.

The algorithm:
1. Voices processed in priority order: required first, then by weight descending.
2. Each sheet instrumentation is claimed by at most one voice (greedy allocation).
3. Per voice: `effectiveCount = Σ(matchScore × option.factor)`, normalized to `targetCount`.
4. A base score of **0.7** ensures any positive match contributes at least 70% of the voice's weight.
5. Overall: weighted average across all voices.

**Match scoring:** instruments must match exactly (by ID). Two optional modifiers can reduce score:
- Clef factor (0.7 for non-native clef)
- Notation-type factor (0.8 for percussion, 0.7 for tablature/graphic)

**Status classification:**

| Status | Condition |
|--------|-----------|
| `INCOMPLETE` | One or more required voices are below `minCount` |
| `PLAYABLE` | All required voices covered; overall score < 85% |
| `COMPLETE` | All required voices covered; overall score ≥ 85% |

### Coverage snapshots

`POST /ensembles/{id}/coverage/compute` precomputes coverage for all sheets and stores the results as **snapshots**. Snapshots are displayed as colour-coded badges in the sheets list without re-evaluating on each page load. Snapshots must be manually recomputed — there is no automatic invalidation on sheet changes.

### Ensemble members

Each ensemble has a **membership roster** — the list of musicians who play in it.

`GET /ensembles/{id}/members` · `POST /ensembles/{id}/members` · `PUT /ensembles/{id}/members/{memberId}` · `DELETE /ensembles/{id}/members/{memberId}`

| Field | Type | Notes |
|-------|------|-------|
| Musician | Musician reference | Required |
| Voice | EnsembleVoice reference | Optional — which voice/part the musician fills |
| Instrument | Instrument reference | Optional — instrument played in this ensemble |
| Conductor | Boolean | Marks the ensemble conductor |

A musician may appear multiple times in the same ensemble (once per voice, for players who double on multiple parts). The `voice_id IS NULL` case is unique per musician per ensemble (prevents duplicate conductor entries). Each member can be linked to a Musician entity that has a `userId` (OIDC subject). A musician may belong to the roster without a login (`userId = null`). For authenticated musicians, ensemble membership is the foundation for the "my parts" view (see roadmap).

When the musician search filter contains text that matches no existing musician, the Add Member dialog shows a **"Create musician '…'"** button. Clicking it calls `POST /musicians` with the typed name, appends the new musician to the local list, auto-selects them, and shows a success toast — no page navigation required.

---

## 10. Shares & Public Access

Resource-scoped share tokens allow specific content to be accessed by unauthenticated users via a URL.

### Share token

A share token links one authenticated creator to one target resource (a sheet instrumentation or a collection). Tokens can carry an optional expiry date and can be revoked at any time.

| Field | Type | Notes |
|-------|------|-------|
| Creator | User ID (OIDC sub) | The authenticated user who created the token |
| Resource type | Enum | `SHEET_INSTRUMENTATION` · `COLLECTION` |
| Resource ID | UUID | The specific resource being shared |
| Expires at | DateTime | Optional; `null` = no expiry |
| Revoked at | DateTime | Set on revocation; `null` = active |

### Share management (authenticated)

`GET /api/shares` · `POST /api/shares` · `DELETE /api/shares/{id}`

The Angular **shares** page lists all tokens created by the current user, showing resource label, creation date, expiry, and status. Actions: **copy link** (copies the public URL to clipboard), **revoke** (immediately invalidates the token).

### Public access (unauthenticated)

`GET /public/share/{token}` — validates the token and returns the resource. No `Authorization` header required.

The Angular **public-share** page renders:
- For a **sheet instrumentation**: instrument name, part label, archive location, condition, and download links for attached documents.
- For a **collection**: programme order, titles, composers, durations, and download links for attached documents.

### Access logging

Every public-share request is logged in `event_log` with `shareTokenId` set and `userId`/`username` as `null`. The event log UI shows "via share link" with the token ID as a tooltip.

---

## 11. Event Log

A write-once access and activity log. Captures read events (downloads, exports) that Hibernate Envers does not track.

### Recorded events

| Event type | Trigger |
|------------|---------|
| `DOCUMENT_DOWNLOAD` | Single document served |
| `DOCUMENT_BATCH_DOWNLOAD` | ZIP or merged-PDF batch download |
| `SHEET_EXPORT` | Sheet exported as JSON, CSV, or ZIP |
| `COLLECTION_EXPORT` | Collection exported |
| `COLLECTION_TOC_GENERATED` | Collection table of contents PDF generated |
| `GEMA_SETLIST_GENERATED` | GEMA setlist xlsx generated |
| `DOCUMENT_CLASSIFIED` | AI classification run on a document |
| `DOCUMENT_CLASSIFICATION_APPLIED` | AI classification result applied |

### Log entry fields

| Field | Notes |
|-------|-------|
| `occurredAt` | Timestamp with timezone |
| `userId` | OIDC subject (null for share-link access) |
| `username` | Snapshotted `preferred_username` (null for share-link access) |
| `eventType` | One of the types above |
| `entityType` / `entityId` | The target entity |
| `metadata` | JSONB payload (filename, count, format, etc.) |
| `shareTokenId` | Set when the event was triggered via a share link; `userId`/`username` are null in that case |

IP addresses are not stored. `userId` + `username` give unambiguous attribution for authenticated access; share-link access is identified by `shareTokenId`.

### Event log UI

`/admin/event-logs` — read-only page with filtering by event type (multi-select), user ID, and entity type.

---

## 12. My Parts

A personalised, read-only view for authenticated musicians showing only the sheets that contain at least one instrumentation for their instrument(s).

### How it works

1. The server resolves the calling user's `userId` (OIDC `sub` claim) to a `Musician` record.
2. All `EnsembleMembership` rows for that musician are collected. The `instrument_id` of each non-null instrument membership is gathered into a set.
3. `GET /api/me/parts` returns sheets that contain **at least one** `Instrumentation` whose `instrument_id` is in that set — regardless of which ensemble voice the musician is assigned to.
4. Each sheet in the response carries a `myInstrumentations` field containing only the subset of that sheet's instrumentations that match the musician's instruments. Other instrumentations are not included.

**Matching is instrument-based, not voice-based.** A doubling musician (Bb Trumpet + Flugelhorn memberships) sees all Bb Trumpet and Flugelhorn instrumentations for every sheet. This is intentional for small ensembles where part assignment is flexible.

### Empty-state cases

| Situation | Response |
|---|---|
| User's OIDC `sub` does not match any `Musician.userId` | Empty list + hint to contact librarian |
| Musician is on the roster but has no instrument assignment (conductor-only) | Empty list |
| No sheets contain the musician's instruments | Empty list |

### Endpoint

`GET /api/me/parts?page=0&size=20` — paginated, sorted alphabetically by title.

### Angular UI

Route `/my-parts` — paginated table with columns: Sheet (title + subtitle, links to sheet detail), Composer, Genre, My Parts (instrument+partLabel chips for each matching instrumentation). Translated as "Meine Stimmen" in German.

---

## 13. Search & Discovery

### Full-text search

`GET /sheets?q=<query>` uses a three-tier scoring strategy:

| Strategy | Technique | Weight |
|----------|-----------|--------|
| Full-text | PostgreSQL `tsvector` + `ts_rank` | 0.70 |
| Trigram | `pg_trgm` similarity on title and composer | 0.20 / 0.10 |
| Phonetic | `dmetaphone` on composer / arranger names | 0.05 |

Results are ranked by combined score. The UI debounces input and paginates results.

### Filter dimensions

| Filter | Description |
|--------|-------------|
| Genre | Dropdown (all distinct genres in the database) |
| Letter | A–Z browser by title first letter (optionally pre-filtered by genre) |
| Ensemble coverage | Filter by coverage status for a selected ensemble |

### Browse

The sheets list supports both **table view** and **card view** (user preference). Each row/card shows coverage badges for all ensembles that have a computed snapshot.

---

## 13. UI & Personalisation

### Layout modes

The Sakai-based layout supports three sidebar modes (persisted in `localStorage`):

| Mode | Description |
|------|-------------|
| Static | Sidebar always visible |
| Overlay | Sidebar overlays content |
| Slim | Icon-only sidebar |

### Theme

- **Light** and **dark** mode, toggleable from the topbar.
- Colours driven by CSS custom properties (PrimeNG Aura preset).

### Language

- **English** (default) and **German** — selectable from the topbar.
- Translations loaded from `public/i18n/{en,de}.json`.
- All UI strings, enum labels, and messages are fully translated in both languages.

### User preferences page

`/user/preferences` — configure layout, theme, and language. Settings persisted in `localStorage`.

---

## Appendix: API surface summary

All endpoints are under the `/api` base path.

| Resource | Base path | Notes |
|----------|-----------|-------|
| Sheets | `/api/sheets` | Includes `/enrich`, `/coverage` |
| Instrumentations | `/api/sheets/{id}/instrumentations` | Sub-resource |
| Sheet documents | `/api/sheets/{id}/documents` | Sub-resource |
| Instrumentation documents | `/api/sheets/{sid}/instrumentations/{iid}/documents` | Sub-resource |
| Global documents | `/api/documents` | Unlinked pool, classify, apply |
| Musicians | `/api/musicians` | |
| Instruments | `/api/instruments` | |
| Sheet collections | `/api/sheet-collections` | Including `/sheets` sub-resource |
| Booklets | `/api/booklets` | Including `/sheets` sub-resource |
| Ensembles | `/api/ensembles` | Including `/coverage/compute`, `/coverage/status` |
| Ensemble voices | `/api/ensembles/{id}/voices` | Sub-resource |
| Voice options | `/api/ensembles/{id}/voices/{vid}/options` | Sub-resource |
| Ensemble members | `/api/ensembles/{id}/members` | Sub-resource |
| Shares | `/api/shares` | Authenticated share management (create, list, revoke) |
| Public share | `/public/share/{token}` | Unauthenticated; token-validated resource access |
| Event log | `/api/event-logs` | Read-only; requires authentication |
| My parts | `/api/me/parts` | Authenticated; paginated sheets for the calling user's instruments |

### Access control

All `/api/*` endpoints require authentication (valid OIDC bearer token). Write operations (POST, PUT, DELETE) additionally require the `music_librarian` or `admin` realm role. Read operations (GET) are accessible to any authenticated user. The `/public/share/{token}` endpoint is explicitly unauthenticated — it validates the share token manually in a separate resource class with no `@Authenticated` class-level annotation. Role enforcement uses `@RolesAllowed` on the JAX-RS implementation classes; the API interface definitions remain role-free to stay usable as a REST client in the CLI module.
