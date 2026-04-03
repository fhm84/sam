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
on each instrumentation. Allows the Notenwart to record where a printed part lives and
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

**Stakeholders:** S1 (Notenwart)
**Effort:** Medium

---

### Condition-based alerts — `idea`

When a configurable number of instrumentations for the same piece are `DAMAGED` or
`LOST`, surface a warning on the sheet detail and optionally in the archive dashboard.
Prevents the Dirigent from scheduling a piece whose physical parts are no longer
usable.

**Stakeholders:** S1 (Notenwart), S2 (Dirigent)
**Effort:** Low–Medium

---

### QR codes on physical folders — `idea`

Each instrumentation entry gets a dedicated URL. The Notenwart can generate a QR code
for that URL and print it as a label for the physical folder. A musician scans the
label → SAM opens the instrumentation detail with archive location, condition, and
attached documents.

No new data model needed — just a QR generation endpoint (e.g.
`GET /api/instrumentations/{id}/qr`) returning a PNG or SVG.

**Stakeholders:** S1 (Notenwart), S3 (Musiker)
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

**Stakeholders:** S1 (Notenwart), S2 (Dirigent)
**Effort:** Medium

---

### Concert programme export — `idea`

Generate a print-ready output (PDF or formatted HTML) from a setlist. Each entry shows:
ordered position, title, composer/arranger, duration, and optional programme notes
(free text per setlist entry).

The Dirigent writes the programme notes; the Notenwart or admin triggers the export.
The output is suitable for printing as a concert booklet or sharing as a public PDF.

Depends on: performance history (for date/event context), setlists (already exist).

**Stakeholders:** S2 (Dirigent)
**Effort:** Medium

---

### Acquisition wish list — `idea`

Any authenticated user can submit an acquisition request: piece title, reason, urgency.
The Notenwart sees a queue and tracks status:

`requested` → `approved` → `ordered` → `received` → `archived`

When a piece transitions to `archived`, it links to the newly created sheet entry.

**Stakeholders:** S2 (Dirigent), S3 (Musiker), S1 (Notenwart)
**Effort:** Low–Medium

---

### Part ordering pipeline — `idea`

When coverage evaluation shows `INCOMPLETE`, the Dirigent or Notenwart can flag a missing
voice as "to order." Track: which part, from which publisher, ordered on, expected
delivery, cost, received on.

Can be implemented as a lightweight status on the voice-level coverage result, or as a
separate procurement entity. Closes the loop between coverage gaps and physical
acquisition.

**Stakeholders:** S1 (Notenwart), S2 (Dirigent)
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

### Musician–instrument assignment within ensemble — `idea`

Link a `Musician` entity to one or more ensemble voices. This enables:

- Automated part distribution lists for the Notenwart before rehearsal
  ("Hand Trumpet 1 folder to Hans, Trumpet 2 to Maria")
- Personal "my parts" view for an authenticated musician
- Resolve the open design question of whether a user account links to a `Musician` entity
  (see `stakeholders.md` Section 6.5)

Data model addition: a join between `Musician` (or a future `User` entity) and
`EnsembleVoice`.

**Stakeholders:** S3 (Musiker), S1 (Notenwart)
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

Before a rehearsal, the Notenwart generates a list: for each instrumentation of a
given piece, which musician should receive which physical folder. Requires
musician–instrument assignment (see above).

Output: a simple printed checklist or PDF.

**Stakeholders:** S1 (Notenwart)
**Effort:** Low (once musician–instrument assignment exists)

---

## 4. Statistics & Reporting

### Archive dashboard — `idea`

A single overview page for the Notenwart and Dirigent summarising archive health:

- Total sheets; % with at least one digital document attached
- Condition breakdown across all instrumentations (GOOD / WORN / DAMAGED / LOST counts)
- Coverage status distribution for the active ensemble (COMPLETE / PLAYABLE / INCOMPLETE)
- Pieces with no digital files (candidates for digitisation)
- Pieces never performed (candidates for review / disposal)
- Most recently added and most recently performed pieces

**Stakeholders:** S1 (Notenwart), S2 (Dirigent), S4 (Administrator)
**Effort:** Medium (aggregation queries + a new dashboard route)

---

### GEMA reporting export — `idea`

SAM already stores ISWC and GEMA work numbers per sheet. Combined with performance
history, an export of performed pieces in a GEMA-compatible format would eliminate
manual reporting for ensembles that currently compile this from paper setlists.

Scope: export performed pieces for a date range as CSV or structured list, including
GEMA work number, title, composer, performance date and event.

Depends on: performance history (see Section 2).

**Stakeholders:** S4 (Administrator), S2 (Dirigent)
**Effort:** Low (once performance history exists)

---

## 5. Access Control & Sharing

### Setlist public page (guest access — minimal) — `planned`

Publish a setlist as a read-only, unauthenticated page at a shareable URL. No login, no
account. The Notenwart or Dirigent marks a collection as "shared" and shares the link
(e.g. via WhatsApp group before a concert).

The page shows: ordered programme, title, composer, duration. No documents, no archive
data.

This is the simplest implementation of S5 (Guest) access and avoids the need for a full
auth model. It is a useful stepping stone before implementing full role-based access
control.

**Stakeholders:** S2 (Dirigent), S5 (Guest)
**Effort:** Low
**Depends on:** None (collections already exist)

---

### Full role-based access control (RBAC) — `planned`

Implement authentication and authorisation across the full application. See
`stakeholders.md` Section 6 for the detailed role model, content visibility options, and
authentication mechanism discussion.

Planned roles: `ADMIN` · `MANAGER` · `CONDUCTOR` · `MUSICIAN` · `GUEST`

Recommended approach: hosted OIDC provider (Auth0 or Google OAuth) + Quarkus OIDC
extension. Content scoping: ensemble-based for musicians, collection-based for guests.

**Stakeholders:** All
**Effort:** High
**Depends on:** Decision on auth provider and content visibility model

---

### Shared document links — `idea`

Allow a specific document (e.g. a scanned part) to be shared via a time-limited or
permanent public URL, independently of full guest access. The Notenwart generates the
link; anyone with it can download the file.

A lightweight alternative to full guest access for ad-hoc file sharing.

**Stakeholders:** S1 (Notenwart), S3b (Guest musician)
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

**Stakeholders:** S1 (Notenwart), S4 (Administrator)
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

**Stakeholders:** S1 (Notenwart)
**Effort:** Low (once internal lending exists)

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
UI-based importer is accessible to non-technical Notenwarte without server access.

**Stakeholders:** S1 (Notenwart), S4 (Administrator)
**Effort:** Medium

---

### Full archive export — `idea`

Export everything — metadata and document files — as a single structured download.
Covers two use cases:

- **Backup:** Off-site copy of the full archive independent of the storage backend.
- **Migration:** Move to a different instance or system without data loss.

Output: a ZIP containing a machine-readable metadata export (JSON or CSV) plus all
stored document files in their original format, organised by sheet / instrumentation.

This is also a trust signal for adoption: ensembles are more willing to commit to SAM
if they know they can take their data with them.

**Stakeholders:** S4 (Administrator), S1 (Notenwart)
**Effort:** Medium

---

### Cost tracking per acquisition — `idea`

Track purchase price and supplier per sheet. Useful for the Notenwart's annual budget
report and for the Administrator to understand archive investment over time.

Fields on `Sheet`: `purchasePrice` (decimal), `supplier` (string), `purchasedOn` (date).
Optionally also on the part ordering pipeline (Section 2) for tracking per-voice costs.

Simple addition — no new entities required. Surfaced in the archive dashboard (Section 4)
as total spend per year or per genre.

**Stakeholders:** S1 (Notenwart), S4 (Administrator)
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

**Stakeholders:** S1 (Notenwart)
**Effort:** Low (trigram query already exists; add a pre-create check endpoint + UI warning)

---

### Notifications / digest — `idea`

Push relevant events to ensemble members through in-app notifications, email, or a
periodic digest. Examples:

| Event | Audience |
|-------|----------|
| New part uploaded for a piece you play | S3 (Musiker) |
| Borrowed part overdue for return | S1 (Notenwart) |
| New sheets added to the archive this week | S2 (Dirigent), S3 |
| Coverage dropped to INCOMPLETE after a voice change | S2 (Dirigent) |
| Acquisition request status changed | Requester |

A weekly digest ("what's new in the archive") is a low-friction starting point before
building a full real-time notification system.

Requires auth (to know who to notify and how). Email delivery via a configurable SMTP
provider; in-app notifications as a second phase.

**Stakeholders:** S1 (Notenwart), S2 (Dirigent), S3 (Musiker)
**Effort:** Medium–High
**Depends on:** Authentication

---

### Audit log UI — `idea`

Hibernate Envers already records all changes. Expose a read-only audit log in the UI:
per entity, show who changed what and when. Useful for the Notenwart to understand
"how did this part end up as LOST?" and for the Administrator to track configuration
changes.

Note: this covers **data mutations** only. Document access is tracked separately — see
below.

**Stakeholders:** S1 (Notenwart), S4 (Administrator)
**Effort:** Medium (query Envers revision tables + new UI component)

---

### Document access log — `idea`

Track who viewed or downloaded which document, and from where. This is a distinct
concern from the Envers audit trail, which only captures data mutations (create / update
/ delete). Document access is a **read event** and requires a separate mechanism.

**What to log per access event:**

| Field | Notes |
|-------|-------|
| `documentId` | Which file was accessed |
| `action` | `VIEW` (inline preview) · `DOWNLOAD` (explicit file save) |
| `timestamp` | When |
| `userId` | Authenticated user ID; `anonymous` before auth is in place |
| `sourceContext` | Where access originated: sheet detail · instrumentation · uploads pool · shared link |
| `ipAddress` | Optional — carries GDPR implications, consider anonymising or omitting |

`sourceContext` is particularly valuable: a download from a shared public link is a very
different signal from one made by an authenticated ensemble member.

**Practical value per stakeholder:**

- **S1 (Notenwart):** Confirm that musicians downloaded their parts before rehearsal.
- **S4 (Administrator):** Detect unusual access patterns (e.g. bulk downloads).
- **Legal / rights:** Evidence of controlled distribution of copyrighted material —
  relevant for GEMA/licensing compliance.
- **Guest access:** Usage telemetry for shared links and public pages.

**Privacy / GDPR:**

Once named user accounts exist, this log constitutes personal data. Design requirements:
- Retention policy (e.g. auto-delete entries older than 12 months)
- Users can view their own access history (right of access)
- IP addresses either omitted or anonymised
- Document the log in the ensemble's privacy policy

> **Implementation note:** This feature should be designed and implemented together with
> authentication — not retrofitted afterwards. A retention policy is significantly easier
> to build in from the start than to add later.

**Stakeholders:** S1 (Notenwart), S4 (Administrator), S2 (Dirigent)
**Effort:** Medium
**Depends on:** Authentication (for meaningful `userId`); GDPR/privacy policy decision on IP address handling

---

## 7. UX & Discovery

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

**Stakeholders:** S2 (Dirigent), S1 (Notenwart)
**Effort:** Medium (backend query builder + UI filter panel)

---

### Thumbnail preview — `idea`

Render the first page of an attached PDF as a thumbnail image. Shown in the sheet list
(card view) and in the sheet detail header so the Notenwart can visually confirm they
are looking at the right score without downloading the full file.

The PDF-to-image rendering infrastructure already exists in `DocumentUtils`
(used for AI vision classification). Thumbnails could be generated on first access and
cached alongside the original document in the storage backend.

**Stakeholders:** S1 (Notenwart), S2 (Dirigent)
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

**Stakeholders:** S1 (Notenwart), S4 (Administrator)
**Effort:** Medium

---

### Recently viewed — `idea`

A quick-access list of the last N sheets viewed by the current user, shown in the
sidebar or as a dedicated widget on the home/dashboard page. Reduces friction for the
Notenwart working on several sheets in sequence during an archiving session.

Can be implemented client-side (localStorage, no backend changes) as a first step, with
server-side persistence as an optional second step once auth is in place.

**Stakeholders:** S1 (Notenwart), S2 (Dirigent)
**Effort:** Low

---

## 8. Open Questions

These are unresolved decisions that will affect multiple features. They should be
answered before the relevant implementation work begins.

| # | Question | Affects |
|---|----------|---------|
| 1 | Should a `Musician` user account link to the existing `Musician` entity, or be a separate `User` entity? | Auth, musician–instrument assignment, "my parts" view |
| 2 | How is "selected content" for guests scoped — per-sheet flag, collection-based sharing, or ensemble-based? | Guest access, setlist public page |
| 3 | Should document-level visibility be independently configurable, or always inherited from the sheet/instrumentation? | Shared document links, guest access |
| 4 | Is anonymous guest access (no link, open public URL) ever desirable? | Guest access scope |
| 5 | Should coverage snapshots be invalidated automatically, or remain manual? | Coverage accuracy, performance |
| 6 | Should lending / checkout be tracked per instrumentation or per physical copy? (Relevant if multiple copies per instrumentation are ever supported) | Lending, physical archive |
| 7 | Which OIDC provider? Self-hosted (Keycloak) or SaaS (Auth0, Google)? | Auth implementation |
| 8 | Should IP addresses be stored in the document access log, or omitted/anonymised? Requires GDPR/privacy policy decision. | Document access log |
| 9 | What retention period for the document access log? (e.g. 12 months) | Document access log |
