# SAM – Roadmap & Ideas

**S**heet music **A**rchiving & **M**anagement

This document collects planned features, ideas under consideration, and open questions.
It is intentionally informal — a living list rather than a project plan.

For the **current feature set** see `features.md`.
For **stakeholder context and use cases** see `stakeholders.md`.
For **technical architecture** see `architecture.md`.

Status values: `idea` · `planned` · `in progress` · `done`

---

## Table of Contents

1. [Physical Archive](#1-physical-archive)
2. [Repertoire Planning](#2-repertoire-planning)
3. [Musician-Facing](#3-musician-facing)
4. [Statistics & Reporting](#4-statistics--reporting)
5. [Access Control & Sharing](#5-access-control--sharing)
6. [Operational & Integration](#6-operational--integration)
7. [UX & Discovery](#7-ux--discovery)
8. [Open Questions](#8-open-questions)

---

## 1. Physical Archive

### Physical location & condition on instrumentations — `done`

Archive location (free text) and condition (`GOOD` / `WORN` / `DAMAGED` / `LOST`) fields
on each instrumentation. Allows the music librarian to record where a printed part lives and
whether it is still usable. Visible in the instrumentation table in the sheet detail view.

---

### Checkout / lending tracking — `idea`

Record when a physical part (or a full set) is borrowed and by whom. Cover two scenarios:

- **Internal lending:** A musician takes a part home for the week. Record: who, which
  instrumentation, borrowed on, expected return.
- **External lending:** Parts lent to a partner ensemble. Record: ensemble name (free
  text or a future `PartnerEnsemble` entity), contact, borrowed on, expected return.

When a part is on loan, its status should be visible in the instrumentation table
alongside the physical condition.

**Stakeholders:** S1 (music librarian)
**Effort:** Medium

---

### Condition-based alerts — `idea`

When a configurable number of instrumentations for the same piece are `DAMAGED` or
`LOST`, surface a warning on the sheet detail and optionally in the archive dashboard.
Prevents the Dirigent from scheduling a piece whose physical parts are no longer
usable.

**Stakeholders:** S1 (music librarian), S2 (Dirigent)
**Effort:** Low–Medium

---

### QR codes on physical folders — `idea`

Each instrumentation entry gets a dedicated URL. The music librarian can generate a QR code
for that URL and print it as a label for the physical folder. A musician scans the
label → SAM opens the instrumentation detail with archive location, condition, and
attached documents.

No new data model needed — just a QR generation endpoint (e.g.
`GET /api/instrumentations/{id}/qr`) returning a PNG or SVG.

**Stakeholders:** S1 (music librarian), S3 (Musiker)
**Effort:** Low

---

## 2. Repertoire Planning

### Performance history — `idea`

Record when and where a piece was performed. Each performance entry links to a sheet
and (optionally) a setlist, and carries a date and event name (free text: "Summer
concert 2024", "Stadtfest Musterstadt").

Unlocks downstream features:
- "When did we last play this?" — visible on the sheet detail
- "What did we play at the summer concert 2023?" — queryable from the collection/setlist
- Input for GEMA reporting (see Section 6)
- Basis for archive statistics (pieces never performed)

**Stakeholders:** S1 (music librarian), S2 (Dirigent)
**Effort:** Medium

---

### Concert programme export — `done`

**Implementation note:** TOC export via `CollectionTocService` is complete; per-entry programme notes (future enhancement).

Generate a print-ready output (PDF or formatted HTML) from a setlist. Each entry shows:
ordered position, title, composer/arranger, duration, and optional programme notes
(free text per setlist entry).

The Dirigent writes the programme notes; the music librarian or admin triggers the export.
The output is suitable for printing as a concert booklet or sharing as a public PDF.

Depends on: performance history (for date/event context), setlists (already exist).

**Stakeholders:** S2 (Dirigent)
**Effort:** Medium

---

### Acquisition wish list — `idea`

Any authenticated user can submit an acquisition request: piece title, reason, urgency.
The music librarian sees a queue and tracks status:

`requested` → `approved` → `ordered` → `received` → `archived`

When a piece transitions to `archived`, it links to the newly created sheet entry.

**Stakeholders:** S2 (Dirigent), S3 (Musiker), S1 (music librarian)
**Effort:** Low–Medium

---

### Part ordering pipeline — `idea`

When coverage evaluation shows `INCOMPLETE`, the Dirigent or music librarian can flag a missing
voice as "to order." Track: which part, from which publisher, ordered on, expected
delivery, cost, received on.

Can be implemented as a lightweight status on the voice-level coverage result, or as a
separate procurement entity. Closes the loop between coverage gaps and physical
acquisition.

**Stakeholders:** S1 (music librarian), S2 (Dirigent)
**Effort:** Medium

---

### Minimum viable setlist — `idea`

Given a subset of musicians present at a specific rehearsal, compute which pieces from
the full repertoire are actually playable tonight. This is the inverse of the standard
coverage evaluation: instead of asking "does this piece work for our full ensemble?",
ask "given who is here, what can we play?"

Flow: Dirigent selects which ensemble voices are occupied tonight (or which musicians are
absent) → SAM re-runs coverage against that reduced configuration → returns a filtered
list of `COMPLETE` or `PLAYABLE` pieces.

Requires musician–instrument assignment (Section 3) as a foundation, or alternatively
a simpler "mark voice as absent" toggle per session.

**Stakeholders:** S2 (Dirigent)
**Effort:** Medium
**Depends on:** Musician–instrument assignment (or a lightweight session-level absence model)

---

### Rehearsal notes per setlist entry — `idea`

A conductor-specific annotation tied not to the piece globally but to a specific
occurrence of it within a setlist: "Focus on bars 32–48, tempo ♩=120, watch the key
change at letter C."

Distinct from the global `notes` field on a sheet, which is permanent archive context.
Rehearsal notes are ephemeral and session-specific — they change from rehearsal to
rehearsal and are only relevant while the setlist is active.

Data model addition: a `notes` field on the `CollectionSheet` join entity (the link
between a collection/setlist and a sheet), which already exists.

**Stakeholders:** S2 (Dirigent)
**Effort:** Low (the join entity already exists; add a field + UI textarea)

---

## 3. Musician-Facing

### Musician profile enrichment — `planned`

Add missing fields to the `Musician` entity surfaced in the Claude Design mockup
(`Create Flows (PrimeNG).html`). Full details in `memory/plan_musician_fields.md`.

Fields to add:
- **email** and **mobile** — contact details for self-service folder access and
  part distribution
- **notes** — free-text textarea, admin-visible only (allergies, vacation patterns,
  instrument quirks)
- **status** — enum (`ACTIVE` / `INACTIVE` / `INVITED` / `PENDING`); drives the
  "Active member" toggle in the UI
- **lastInviteSentAt** — timestamp tracking whether an invite email was sent for
  self-service onboarding
- **role** — enum (`MEMBER` / `GUEST` / `SUBSTITUTE` / `CONDUCTOR`); general role
  independent of per-ensemble `conductor` flag on `EnsembleMembership`
- **global instruments** — new `musician_instruments` junction table linking musicians
  to the instruments they *can play*, with an `isPrimary` flag; distinct from the
  per-ensemble `EnsembleMembership.instrumentId`

**Decision:** `name` field stays as a single full-name string — no first/last split.

**Stakeholders:** S1 (music librarian), S3 (Musiker)
**Effort:** Medium

---

### Musician–instrument assignment within ensemble — `done`

Link a `Musician` entity to an ensemble, optionally specifying which voice and instrument
they play. Enables:

- Automated part distribution lists ("Hand Trumpet 1 folder to Hans")
- Personal "my parts" view for an authenticated musician
- Minimum-viable setlist (see Section 2)

**Done:** `EnsembleMembership` data model (musician + ensemble + voice + instrument +
conductor flag), REST API (`/ensembles/{id}/members`), management UI in the ensemble
detail page, and the "My Parts" view (`GET /api/me/parts`, Angular route `/my-parts`).
Musicians are linked to system accounts via `userId` (OIDC subject) on the `Musician`
entity — no separate `User` entity.

**Matching strategy:** instrument-based across all memberships. A musician who doubles
(e.g. Bb Trumpet + Flugelhorn) sees all matching instrumentations. Results are grouped
per sheet — one row per sheet with all matching parts shown as chips.

**Still needs:** frontend OIDC integration to wire the actual logged-in user's `sub` claim
into the API call. Until then the endpoint returns an empty list for all authenticated
users (the security enforcement is in place; the JWT subject plumbing is the missing piece
— see Full RBAC remaining work).

**Stakeholders:** S3 (Musiker), S1 (music librarian)
**Effort:** Medium–High (requires auth foundation)

---

### Mobile-first quick-lookup view — `idea`

A stripped-down, mobile-optimised page showing only what a musician needs while standing
at the archive cabinet:

- Piece title
- Instrument / part label
- Archive location
- Condition badge

Accessible via the QR code on the physical folder or via direct search. No metadata
panels, no coverage, no batch actions — just the essentials readable at arm's length.

**Stakeholders:** S3 (Musiker)
**Effort:** Low (new route + minimal component, reuses existing API)

---

### Part distribution list — `idea`

Before a rehearsal, the music librarian generates a list: for each instrumentation of a
given piece, which musician should receive which physical folder. Requires
musician–instrument assignment (see above).

Output: a simple printed checklist or PDF.

**Stakeholders:** S1 (music librarian)
**Effort:** Low (once musician–instrument assignment exists)

---

## 4. Statistics & Reporting

### Home dashboard — `planned`

A home page replacing the current empty landing screen. Two tabs, derived from the
Claude Design `Hi-Fi Shell (PrimeNG).html`. Full details in `memory/plan_home_dashboard.md`.

**Inbox tab:**
- KPI row: to-classify count, instrumentations missing archive location, stale coverage
  ensembles (≥7 days), new sheets this week
- Activity feed (recent event log entries with display labels)
- Right rail: quick-upload widget, "1 voice from playable" gap card, next concert card
  (requires new `venue` field on `SheetCollection`)

**Ensemble dashboard tab:**
- Coverage KPI row (complete / playable / incomplete counts + total repertoire)
- Most-missing voices card (gap report across full repertoire)
- One-voice-away card (sheets within N% of PLAYABLE threshold)

**New data model fields required:**
- `venue` (String) on `SheetCollection`
- `MEMBER_JOINED` / `MEMBER_LEFT` added to `EventType` enum
- Coverage staleness threshold config (`sam.coverage.stale-threshold-days`)

**Stakeholders:** S1 (music librarian), S2 (Dirigent), S4 (Administrator)
**Effort:** High
**Depends on:** Coverage snapshots, event log, ensemble memberships

---

### Archive dashboard — `idea`

A single overview page for the music librarian and Dirigent summarising archive health:

- Total sheets; % with at least one digital document attached
- Condition breakdown across all instrumentations (GOOD / WORN / DAMAGED / LOST counts)
- Coverage status distribution for the active ensemble (COMPLETE / PLAYABLE / INCOMPLETE)
- Pieces with no digital files (candidates for digitisation)
- Pieces never performed (candidates for review / disposal)
- Most recently added and most recently performed pieces

**Stakeholders:** S1 (music librarian), S2 (Dirigent), S4 (Administrator)
**Effort:** Medium (aggregation queries + a new dashboard route)

---

### GEMA reporting export — `in progress`

**Implementation note:** GEMA setlist template generation via `GemaSetlistService` (Apache POI xlsx) is complete.
Performance history tracking (dependencies) is still pending — needed for date-range reporting.

SAM already stores ISWC and GEMA work numbers per sheet.

**Stakeholders:** S4 (Administrator), S2 (Dirigent)
**Effort:** Low (once performance history exists)

---

## 5. Access Control & Sharing

### Setlist public page (guest access — minimal) — `done`

**Implementation note:** Implemented as part of the Share Links feature (see below). The
music librarian creates a resource-scoped share token for a collection via
`POST /api/shares`. The resulting public URL (`/public/share/{token}`) renders the
collection's programme — title, composer, duration — without requiring login. Download
links for attached documents are also included (a superset of the original spec).

Publish a setlist as a read-only, unauthenticated page at a shareable URL. No login, no
account. The music librarian or Dirigent marks a collection as "shared" and shares the link
(e.g. via WhatsApp group before a concert).

This is the simplest implementation of S5 (Guest) access and avoids the need for a full
auth model.

**Stakeholders:** S2 (Dirigent), S5 (Guest)
**Effort:** Low
**Depends on:** None (collections already exist)

---

### Full role-based access control (RBAC) — `in progress`

**Done (Phase 1–3):**
- `EnsembleMembership` data model with `userId` on `Musician` for OIDC identity linking
- Quarkus OIDC extension configured (`quarkus-oidc`); dev profile points to local Keycloak 26
- Keycloak realm export (`keycloak/sam-realm.json`) with roles, test users, and groups claim mapper
- `docker-compose.keycloak.yml` for local development
- `CurrentUserService` — reads JWT subject, realm roles, and ensemble group membership (`ensemble:{UUID}` flat groups)
- `@Authenticated` + `@RolesAllowed` enforced on all resource implementation classes; test profile uses `%test.quarkus.oidc.enabled=false` + `@TestSecurity` for auth-specific tests

**Roles implemented:** `admin` · `music_librarian` (Keycloak realm roles)

**Remaining (Phase 4+):**
- Frontend OIDC integration (Angular + Keycloak JS adapter or PKCE flow)
- Role-aware UI (hide write actions for read-only users, show only accessible ensembles)
- "My parts" view scoped to the logged-in musician's ensemble memberships
- Conductor role surfaced in the UI (currently stored in data model, not yet used for access control)

**Stakeholders:** All
**Effort:** High
**Auth provider chosen:** Self-hosted Keycloak 26 (`ensemble:{UUID}` groups for per-ensemble access)

---

### Shared document links — `done`

**Implementation note:** Fully implemented. The `shares` table stores resource-scoped
tokens (one token = one resource: a sheet instrumentation or a collection).
`POST /api/shares` creates a token; `GET /public/share/{token}` is the unauthenticated
endpoint. The Angular `shares` page lists all tokens for the current user with copy-link
and revoke actions. The `public-share` page renders the resource for unauthenticated
visitors with download links. All share-link access is logged in `event_log` with the
`shareTokenId` column (userId/username set to null on share-link requests).

Allow a specific document (e.g. a scanned part) to be shared via a time-limited or
permanent public URL, independently of full guest access. The music librarian generates the
link; anyone with it can download the file.

Tokens are resource-scoped (one token = one resource), not broad API keys.

**Stakeholders:** S1 (music librarian), S3b (Guest musician)
**Effort:** Low–Medium

---

### Collection visibility & cover — `planned`

Add two missing fields to `SheetCollection` surfaced in the Claude Design mockup
(`Create Flows (PrimeNG).html`). Full details in `memory/plan_collection_fields.md`.

- **visibility** — enum (`WHOLE_ENSEMBLE` / `ADMINS_ONLY` / `PRIVATE`); controls
  who can see the collection. The design shows a "Whole ensemble" dropdown in the
  create dialog. Will interact with `CurrentUserService.getAccessibleEnsembleIds()`.
- **coverColor** — string (hex or named swatch); displayed as an initial-based
  gradient tile in the collection list.
- **coverImageId** — nullable UUID reference to an uploaded document/attachment;
  overrides the color swatch when set.

**Decision:** `CollectionType` enum stays as-is (`FOLDER` / `SETLIST`). The UI maps
design labels: Concert → `SETLIST`; Season / Rehearsal / Custom → `FOLDER`.

**Stakeholders:** S1 (music librarian), S2 (Dirigent)
**Effort:** Low–Medium

---

### Watermarking on shared / downloaded documents — `idea`

When a document is downloaded via a shared link or a guest-accessible URL, optionally
overlay a watermark on the PDF: e.g. "Property of [Ensemble Name] — for rehearsal use
only" or "Not for redistribution."

The watermark text is configurable per ensemble. Applied on-the-fly at download time
(the stored original is never modified). Relevant for copyright compliance and controlled
distribution of licensed material.

Implementation: PDF watermarking via Apache PDFBox (already a dependency for text
extraction).

**Stakeholders:** S1 (music librarian), S4 (Administrator)
**Effort:** Medium
**Depends on:** Shared document links or guest access

---

## 6. Operational & Integration

### Lending to partner ensembles — `idea`

Track when physical parts are lent to another ensemble. Record: partner name, contact,
lent on, expected return, returned on. Parts on external loan are flagged in the
instrumentation table and in coverage evaluation (a lent part is temporarily unavailable).

Can be implemented as an extension of the checkout/lending feature (Section 1) with an
"external" flag.

**Stakeholders:** S1 (music librarian)
**Effort:** Low (once internal lending exists)

---

### Coverage breakdown enhancements — `planned`

Extend the coverage engine to match the `Coverage Breakdown (PrimeNG).html` design.
Full details in `memory/plan_coverage_breakdown.md`.

- **Condition/substitute annotations** — surface `conditionPenalty` and
  `substituteFactor` as named fields on `VoiceCoverageDetail` (values already computed)
- **Multi-ensemble context view** — `GET /sheets/{id}/coverage` returns snapshots for
  all ensembles at once, enabling side-by-side comparison
- **Ensemble gap report** — `GET /ensembles/{id}/gaps` with per-voice missing count
  across the full repertoire; shared with home dashboard "most-missing voices" widget
- **Recommendation solver** — which 1–2 voices, if added, would move the most sheets
  from INCOMPLETE → PLAYABLE

**Stakeholders:** S2 (Dirigent), S1 (music librarian)
**Effort:** Medium

---

### Classification queue — `planned`

A batch inbox for working through a queue of unclassified documents. Shown in the
Claude Design `Classify (PrimeNG).html`. Full details in `memory/plan_classify_queue.md`.

The queue sits on top of the existing 2-step classify/apply workflow and adds:
- Inbox tabs: Pending / Skipped / Done
- Per-item confidence ordering and status tracking
- Progress bar with estimated time
- Pause/resume batch control
- Three layout modes: Split (rail + viewer + form), Stack (card-flip), Chat (agent)

Five open design questions must be resolved before implementation begins (see the
plan file). This is a large standalone feature.

**Stakeholders:** S1 (music librarian)
**Effort:** High
**Depends on:** Classification enhancements (see below)

---

### Classification form enhancements — `planned`

Enrich the existing 2-step classify/apply workflow with richer AI output and a more
complete apply request. Full details in `memory/plan_classify_enhancements.md`.

- **Tags + notes** in `ClassificationApplyRequest` — both shown in the form but absent from the DTO
- **Document type** (Part / Score / Solo) returned by AI analysis
- **Field-level confidence scores** — per-field (title, composer, instrument, etc.)
- **Multiple ranked alternatives** for sheet match and instrument match
- **Sanity check validation** — pre-apply warnings (duplicate instrumentation, missing
  archive location, instrument mismatch)
- **Re-analyse endpoint** — re-run AI on an already-classified document

**Stakeholders:** S1 (music librarian)
**Effort:** Medium
**Depends on:** None (extends existing classification)

---

### Sheet create wizard — `planned`

Three fields shown in the `Create Flows (PrimeNG).html` create wizard are missing from
the data model. Full details in `memory/plan_create_sheet.md`.

- **Pages per part** — free-text string (e.g. "1–2", "6") on `Instrumentation`
- **Source** — free-text string on `SheetMusic` (where the piece came from)
- **Collection link at create time** — `collectionId` on `CreateSheetMusic` to assign
  the sheet to a collection in one step

**Stakeholders:** S1 (music librarian)
**Effort:** Low

---

### Automatic coverage snapshot invalidation — `planned`

Currently, coverage snapshots must be manually recomputed after changes. Implement
automatic invalidation (and optional recomputation) when a sheet or instrumentation is
created, updated, or deleted.

This is already noted as a known gap in the architecture (`instruments_and_ensembles.md`,
Phase 3).

**Stakeholders:** S2 (Dirigent), S4 (Administrator)
**Effort:** Medium

---

### Sheet metadata enrichment — `planned`

Add missing fields to `SheetMusicEntity` and related entities surfaced in the Claude
Design mockup (`Sheet Detail (PrimeNG).html` / `Sheet Detail v2 (PrimeNG).html`).
Full details in `memory/plan_sheet_detail_fields.md`.

**Sheet-level fields:**
- **tempo** (Integer, bpm) — "♩= 116 bpm" shown in Base Data card
- **tonality** — fixed enum of all major/minor keys (e.g. Bb major, F minor)
- **rightsStatus** — enum (`PUBLIC_DOMAIN` / `LICENSED` / `RESTRICTED` / `UNKNOWN`)
- **gemaPflichtig** — 3-state enum (`YES` / `NO` / `UNKNOWN`)
- **arrangementPublisher** + **arrangementRightsUntil** — simple fields for
  "Musikverlag Tirol · until 2080"-style arranger rights; no separate entity

**Attachment-level fields:**
- **AttachmentKind** — new enum (`CLEAN` / `MARKED_UP` / `PHOTOCOPY` / `FACSIMILE` /
  `SCORE`) as a separate field alongside the existing `AttachmentType`
- **version** (Integer) + **replacedById** (self-referencing FK) — for v1/v2/v3
  file lineage tracking

**Collection:**
- **ensemble FK on SheetCollection** — explicit link to which ensemble a setlist
  belongs to (currently implied; the "Used in setlists" panel shows the ensemble name)

**Deferred:** Duplicate / Merge / Split sheet operations (More menu in v2).

**Stakeholders:** S1 (music librarian), S2 (Dirigent), S4 (Administrator)
**Effort:** Medium

---

### Instrument catalogue enrichment — `planned`

Add missing fields to the `Instrument` entity surfaced in the Claude Design mockup
(`Create Flows (PrimeNG).html`). Full details in `memory/plan_instrument_fields.md`.

Fields to add:
- **family** — fixed enum (`BRASS` / `WOODWIND` / `STRING` / `PERCUSSION` /
  `KEYBOARD` / `VOICE` / `OTHER`). The `family` field was already anticipated
  (commented-out stub in `Instrument.java` / `CreateInstrument.java`) but never
  implemented.
- **defaultClef** — reuses the existing `Clef` enum (`TREBLE` / `ALTO` / `TENOR` /
  `BASS`), added at the instrument level (currently only used at the instrumentation
  level).
- **OCR aliases** — a one-to-many `instrument_aliases` table; multi-value token
  field in the UI (e.g. "Flügelhorn", "Flh.", "Flugelhorn"). Used by the AI
  classification pipeline to match imported documents to instruments.
- **catalogSection** and **catalogPosition** — string + integer fields that drive
  the "Brass · High / #7" ordering shown in part lists and ensemble setups.

**Stakeholders:** S1 (music librarian)
**Effort:** Low–Medium
**Depends on:** Instrument catalogue enrichment unlocks better OCR import matching

---

### Excel / CSV import — `idea`

An import wizard that reads a spreadsheet (Excel or CSV) of existing archive inventory
and creates sheets in bulk. Most ensembles currently maintain their catalogue in Excel
— this is the primary migration path into SAM for new adopters.

Features:
- Column mapping UI (match spreadsheet columns to SAM fields)
- Validation with per-row error reporting before committing
- Conflict detection: flag rows where a sheet with the same title + composer already exists
- Dry-run mode: preview what would be created without committing

The CLI already supports batch import via REST, but requires technical setup. A
UI-based importer is accessible to non-technical music librariane without server access.

**Stakeholders:** S1 (music librarian), S4 (Administrator)
**Effort:** Medium

---

### Full archive export — `done`

**Implementation note:** Sheet and collection export (ZIP/JSON/CSV) is implemented. Full archive export with all document files is a future enhancement.
Covers two use cases:

- **Backup:** Off-site copy of the full archive independent of the storage backend.
- **Migration:** Move to a different instance or system without data loss.

Output: a ZIP containing a machine-readable metadata export (JSON or CSV) plus all
stored document files in their original format, organised by sheet / instrumentation.

This is also a trust signal for adoption: ensembles are more willing to commit to SAM
if they know they can take their data with them.

**Stakeholders:** S4 (Administrator), S1 (music librarian)
**Effort:** Medium

---

### Cost tracking per acquisition — `idea`

Track purchase price and supplier per sheet. Useful for the music librarian's annual budget
report and for the Administrator to understand archive investment over time.

Fields on `Sheet`: `purchasePrice` (decimal), `supplier` (string), `purchasedOn` (date).
Optionally also on the part ordering pipeline (Section 2) for tracking per-voice costs.

Simple addition — no new entities required. Surfaced in the archive dashboard (Section 4)
as total spend per year or per genre.

**Stakeholders:** S1 (music librarian), S4 (Administrator)
**Effort:** Low

---

### Duplicate detection on create — `idea`

When creating a new sheet, check for near-duplicates before saving — not just exact
fingerprint matches (already enforced at DB level) but fuzzy matches on title + composer
using the existing trigram infrastructure.

If similar sheets are found, show a warning: "A sheet with a similar title already
exists — are you sure this is a different piece?" The user can dismiss and proceed, or
navigate to the existing entry.

Prevents the archive from accumulating near-duplicate entries due to slight spelling
differences or different edition names for the same work.

**Stakeholders:** S1 (music librarian)
**Effort:** Low (trigram query already exists; add a pre-create check endpoint + UI warning)

---

### Notifications / digest — `idea`

Push relevant events to ensemble members through in-app notifications, email, or a
periodic digest. Examples:

| Event | Audience |
|-------|----------|
| New part uploaded for a piece you play | S3 (Musiker) |
| Borrowed part overdue for return | S1 (music librarian) |
| New sheets added to the archive this week | S2 (Dirigent), S3 |
| Coverage dropped to INCOMPLETE after a voice change | S2 (Dirigent) |
| Acquisition request status changed | Requester |

A weekly digest ("what's new in the archive") is a low-friction starting point before
building a full real-time notification system.

Requires auth (to know who to notify and how). Email delivery via a configurable SMTP
provider; in-app notifications as a second phase.

**Stakeholders:** S1 (music librarian), S2 (Dirigent), S3 (Musiker)
**Effort:** Medium–High
**Depends on:** Authentication

---

### Audit log UI — `idea`

Hibernate Envers already records all changes. Expose a read-only audit log in the UI:
per entity, show who changed what and when. Useful for the music librarian to understand
"how did this part end up as LOST?" and for the Administrator to track configuration
changes.

Note: this covers **data mutations** only. Document access is tracked separately — see
below.

**Stakeholders:** S1 (music librarian), S4 (Administrator)
**Effort:** Medium (query Envers revision tables + new UI component)

---

### Document access log — `partial`

Track who viewed or downloaded which document, and from where. This is a distinct
concern from the Envers audit trail, which only captures data mutations (create / update
/ delete). Document access is a **read event** and requires a separate mechanism.

**Implemented (Phase 1):**

An `event_log` table and `GET /api/event-logs` endpoint are in place. The following
events are currently recorded:

| Event type | Trigger |
|------------|---------|
| `DOCUMENT_DOWNLOAD` | Single document served via `GET /documents/{id}` |
| `DOCUMENT_BATCH_DOWNLOAD` | ZIP or merged-PDF batch download |
| `SHEET_EXPORT` | Sheet exported as JSON, CSV, or ZIP |
| `COLLECTION_EXPORT` | Collection exported |
| `COLLECTION_TOC_GENERATED` | Collection table of contents PDF generated |
| `GEMA_SETLIST_GENERATED` | GEMA setlist xlsx generated |
| `DOCUMENT_CLASSIFIED` | AI classification run on a document |
| `DOCUMENT_CLASSIFICATION_APPLIED` | AI classification result applied to create entities |

Each event captures: `occurredAt`, `userId` (OIDC subject), `username` (snapshotted
`preferred_username` at event time), `eventType`, `entityType`, `entityId`, and a
`metadata` JSONB payload (e.g. filename, count, format). For share-link access,
`userId`/`username` are null and `shareTokenId` is set instead (see Shared document
links below). A read-only UI page is available at `/admin/event-logs` with filtering
by event type (multi-select), user ID, and entity type.

IP addresses are deliberately not stored — the `userId`+`username` pair gives
unambiguous attribution for all authenticated users, and IP logging would add GDPR
compliance obligations without meaningful benefit for an ensemble-management context.

**Still pending:**

- Richer UI (charts, per-entity history panel, date-range filter)
- Retention policy (auto-delete entries older than N months)
- Users viewing their own access history (GDPR right of access)

**Privacy / GDPR:**

Once named user accounts exist, this log constitutes personal data:
- Retention policy (e.g. auto-delete entries older than 12 months)
- Users can view their own access history (right of access)
- Document the log in the ensemble's privacy policy

**Stakeholders:** S1 (music librarian), S4 (Administrator), S2 (Dirigent)
**Depends on:** Authentication (for meaningful `userId`)

---

## 7. UX & Discovery

### Sheets overview filter & bulk actions — `planned`

Extend the sheet list API and Angular UI to match the filter toolbar and bulk actions
shown in the Claude Design `Sheets Overview (PrimeNG).html`. Full details in
`memory/plan_sheets_overview.md`.

**Missing filter dimensions** (to add to `SheetFilterRequest`):
- Coverage status filter (COMPLETE / PLAYABLE / INCOMPLETE, per ensemble)
- Difficulty level filter (multi-select)
- Duration range filter (min/max)
- Tags filter (multi-select, AND or OR)
- "Has issues" flag (sheets with DAMAGED/LOST parts or INCOMPLETE coverage)

**Missing sort:**
- Composer sort (not currently in `ALLOWED_SORT_FIELDS`)

**Missing bulk actions** (new endpoints):
- Bulk add to setlist (`POST /collections/{id}/items/bulk`)
- Bulk archive (`POST /sheets/bulk-archive`)
- Bulk export (`POST /sheets/bulk-export`)

**Stakeholders:** S1 (music librarian), S2 (Dirigent)
**Effort:** Medium

---

### Advanced combined search — `idea`

A filter builder that combines multiple dimensions in a single query. Currently filters
(genre, letter, coverage status) are independent and cannot be composed.

Example query: *"All marches, difficulty 3–4, COMPLETE for Ensemble A, not performed
in the last 2 years."*

Proposed filter dimensions:

| Dimension | Type |
|-----------|------|
| Full-text query | Free text |
| Genre | Multi-select |
| Style | Multi-select |
| Difficulty level | Range (1–6) |
| Coverage status | Select (per ensemble) |
| Has digital files | Boolean |
| Physical condition | Select |
| Last performed | Date range / "never" |
| Tags | Multi-select |
| Composer / arranger | Free text |

The filter state should be shareable as a URL so a Dirigent can bookmark a recurring
query (e.g. "playable marches for the summer concert shortlist").

**Stakeholders:** S2 (Dirigent), S1 (music librarian)
**Effort:** Medium (backend query builder + UI filter panel)

---

### Thumbnail preview — `idea`

Render the first page of an attached PDF as a thumbnail image. Shown in the sheet list
(card view) and in the sheet detail header so the music librarian can visually confirm they
are looking at the right score without downloading the full file.

The PDF-to-image rendering infrastructure already exists in `DocumentUtils`
(used for AI vision classification). Thumbnails could be generated on first access and
cached alongside the original document in the storage backend.

**Stakeholders:** S1 (music librarian), S2 (Dirigent)
**Effort:** Low–Medium (reuses existing rendering; add caching + UI)

---

### Bulk metadata edit — `idea`

Select multiple sheets from the list and edit a shared field across all of them in one
operation. Use cases:

- Set genre for a batch of newly imported sheets
- Apply a tag to all pieces in a setlist
- Set difficulty level for a group of similar pieces
- Mark a set of sheets as favorites

The edit applies only to fields explicitly changed — a "partial update" that leaves
unspecified fields untouched on each selected sheet.

**Stakeholders:** S1 (music librarian), S4 (Administrator)
**Effort:** Medium

---

### Recently viewed — `idea`

A quick-access list of the last N sheets viewed by the current user, shown in the
sidebar or as a dedicated widget on the home/dashboard page. Reduces friction for the
music librarian working on several sheets in sequence during an archiving session.

Can be implemented client-side (localStorage, no backend changes) as a first step, with
server-side persistence as an optional second step once auth is in place.

**Stakeholders:** S1 (music librarian), S2 (Dirigent)
**Effort:** Low

---

## 8. Open Questions

These are unresolved decisions that will affect multiple features. They should be
answered before the relevant implementation work begins.

| # | Question | Affects | Status |
|---|----------|---------|--------|
| 1 | Should a `Musician` user account link to the existing `Musician` entity, or be a separate `User` entity? | Auth, musician–instrument assignment, "my parts" view | **Resolved:** `userId` (OIDC subject) added to `Musician` — no separate User entity. External/historical musicians have `userId = null`. |
| 2 | How is "selected content" for guests scoped — per-sheet flag, collection-based sharing, or ensemble-based? | Guest access, setlist public page | **Resolved:** Resource-scoped share tokens implemented (one token = one sheet instrumentation or collection). Public setlist/sheet pages live at `/public/share/{token}`. Open-URL anonymous access (no link) intentionally deferred. |
| 3 | Should document-level visibility be independently configurable, or always inherited from the sheet/instrumentation? | Shared document links, guest access | Open |
| 4 | Is anonymous guest access (no link, open public URL) ever desirable? | Guest access scope | Open |
| 5 | Should coverage snapshots be invalidated automatically, or remain manual? | Coverage accuracy, performance | Open |
| 6 | Should lending / checkout be tracked per instrumentation or per physical copy? (Relevant if multiple copies per instrumentation are ever supported) | Lending, physical archive | Open |
| 7 | Which OIDC provider? Self-hosted (Keycloak) or SaaS (Auth0, Google)? | Auth implementation | **Resolved:** Self-hosted Keycloak 26. |
| 8 | Should IP addresses be stored in the document access log, or omitted/anonymised? Requires GDPR/privacy policy decision. | Document access log | **Resolved: not stored.** `userId` (OIDC sub) + snapshotted `username` give unambiguous attribution; IP adds GDPR obligations without meaningful benefit in an ensemble context. |
| 9 | What retention period for the document access log? (e.g. 12 months) | Document access log | Open |
| 10 | All Claude Design files have now been reviewed for data model gaps. Resulting plans saved to `memory/plan_*.md` and added to the roadmap. | Multiple | **Resolved** |
