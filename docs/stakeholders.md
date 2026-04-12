# SAM – Stakeholders, Use Cases & Flows

**S**heet music **A**rchiving & **M**anagement

This document describes who uses SAM, what they need from it, and how the key workflows
look in practice. It complements `architecture.md` (technical structure) and
`features.md` (feature reference) with a human-centred perspective.

---

## Table of Contents

1. [Stakeholders](#1-stakeholders)
2. [Quality Goals](#2-quality-goals)
3. [System Context](#3-system-context)
4. [Use Cases](#4-use-cases)
5. [Key Flows](#5-key-flows)
6. [Access Control Model](#6-access-control-model)
7. [Glossary](#7-glossary)

---

## 1. Stakeholders

### Overview

| ID | Role | German term | Primary concern | Auth |
|----|------|-------------|-----------------|------|
| S1 | Archive Manager | Notenwart | Where is everything? Is it usable? | authenticated, full write |
| S2 | Conductor | Dirigent | What can we play? What do we need? | authenticated, read + planning |
| S3 | Musician | Musiker | Where is my part? | authenticated, scoped read |
| S4 | Administrator | Administrator | Is the system set up correctly? | authenticated, full access |
| S5 | Guest | Gast | See what the ensemble plays / find a specific piece | unauthenticated or link-only |

> **Role overlap:** In smaller ensembles the Notenwart and Dirigent are frequently the
> same person. Use cases for both roles should be considered when designing workflows for
> that combined user.

> **Current state:** SAM does not yet implement authentication or authorisation. All
> roles currently have unrestricted access. The model described here reflects the
> **planned** access structure. See [Section 6](#6-access-control-model) for design
> considerations.

---

### S1 – Archive Manager (Notenwart)

The Music librarian is responsible for maintaining the ensemble's sheet music archive — both
physical (folders, cabinets) and digital. This is often a voluntary position, filled by
someone with a strong sense of order rather than technical expertise.

**Goals**
- Know exactly where every printed part is stored (cabinet, shelf, folder)
- Know the condition of every physical copy (good, worn, damaged, lost)
- Keep the digital archive in sync with the physical one
- Ensure new acquisitions are catalogued promptly

**Pain points (before SAM)**
- Parts go missing and nobody records it
- No single source of truth for "do we own this piece?"
- Digitising physical scores is tedious without AI assistance
- Condition of old copies only becomes apparent at rehearsal

**SAM features most relevant**
- Sheet and instrumentation management (create, edit)
- Physical archive location + condition fields on instrumentations
- Document upload and AI classification
- Staging area (unlinked document pool)

---

### S2 – Conductor (Dirigent)

The Dirigent decides what the ensemble plays. They need to know not just whether a piece
exists in the archive, but whether the ensemble has *all the parts needed* to actually
perform it.

**Goals**
- Quickly assess playability of a piece for the current ensemble
- Browse repertoire by genre, difficulty, or style when planning a concert
- Build and manage setlists for specific concerts or events
- Identify gaps (missing voices) and commission new parts if needed

**Pain points (before SAM)**
- No quick way to check if all parts are available for a given arrangement
- Concert planning is done by memory or paper lists
- Discovering a missing part at rehearsal wastes everyone's time

**SAM features most relevant**
- Ensemble coverage evaluation (COMPLETE / PLAYABLE / INCOMPLETE)
- Search and filter (genre, difficulty, coverage status)
- Collections and setlists
- Coverage breakdown per voice (which parts are missing)

---

### S3 – Musician (Musiker)

The Musiker needs their specific part — either physically from the archive folder or as a
digital file for home practice. Their interaction with SAM is typically read-only and
goal-directed: find the right file, download it, done.

There are two sub-variants with different access expectations:

- **S3a — Authenticated musician:** A registered ensemble member with a personal login.
  Sees the full repertoire of their ensemble(s). Can download any part. Their user
  account may be linked to an existing `Musician` entity in the data model.
- **S3b — Guest musician (anonymous):** Uses a shared link or a public URL to access
  a specific sheet or collection. No account required. Access is intentionally limited
  to what the ensemble has chosen to share.

**Goals**
- Locate the physical folder for their instrument part before rehearsal
- Download their digital part for practice at home
- Quickly verify a piece is in the repertoire

**Pain points (before SAM)**
- Physical parts are unlabelled or misfiled
- No way to access digital files without asking the Music librarian
- Uncertainty whether a digital version even exists

**SAM features most relevant**
- Sheet search (scoped to ensemble repertoire for authenticated musicians)
- Instrumentation detail (physical location)
- Document download (individual parts)

---

### S4 – Administrator

The Administrator keeps the reference data consistent and ensures the system is correctly
configured. This is often the same person as the Music librarian or a technically-minded
ensemble member.

**Goals**
- Maintain a clean, canonical instrument catalogue
- Define ensemble voice structures that reflect the actual ensemble composition
- Keep coverage snapshots current after structural changes
- Manage user accounts and role assignments
- Control which content is visible to guests and authenticated musicians

**SAM features most relevant**
- Instruments catalogue management
- Ensemble and voice definition management
- Coverage snapshot recomputation
- User and role management *(planned)*
- Content visibility / sharing settings *(planned)*

---

### S5 – Guest (Gast)

A Guest has no account in SAM. They access the system via a shared link, a public URL,
or an embedded view. Typical cases: a concert audience member looking up the programme,
a partner ensemble checking what the band plays, or a musician who has not yet registered.

**Goals**
- See a curated view of the ensemble's repertoire (e.g. a setlist for tonight's concert)
- Find a specific piece without logging in
- Optionally download a publicly shared part or score

**Pain points (without guest access)**
- No way to share repertoire information without giving full system access
- Music librarian must export PDFs or send files manually when asked

**Constraints**
- No write access of any kind
- Access is limited to what an Admin/Music librarian has explicitly marked as visible
- May be further restricted to specific collections, sheets, or even individual documents

**SAM features most relevant**
- Public/shared collection view *(planned)*
- Read-only sheet detail *(planned — scoped)*
- Document download where explicitly permitted *(planned)*

---

## 2. Quality Goals

These goals are derived directly from stakeholder needs. They are listed in priority order.

| Priority | Goal | Linked to | Approach |
|----------|------|-----------|----------|
| 1 | **Findability** | S1, S2, S3 | Full-text search with trigram + phonetic fallback; physical location on each part |
| 2 | **Playability assessment** | S2 | Coverage evaluation engine with ensemble voice matching |
| 3 | **Archive integrity** | S1, S4 | Fingerprint-based deduplication; condition tracking; audit trail |
| 4 | **Low friction for archiving** | S1 | AI-assisted classification reduces manual data entry |
| 5 | **Traceability** | S1, S4 | Full audit trail via Hibernate Envers on all entities |

---

## 3. System Context

```
  ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
  │  S1 Notenwart    │    │  S2 Dirigent     │    │  S3 Musiker      │
  │  (Archive Mgr)   │    │  (Conductor)     │    │  (Musician)      │
  └────────┬─────────┘    └────────┬─────────┘    └────────┬─────────┘
           │                       │                        │
           │  manages archive      │  plans repertoire      │  finds parts
           │  uploads, classifies  │  checks coverage       │  downloads files
           │                       │                        │
           └───────────────────────┼────────────────────────┘
                                   │
                            ┌──────▼──────┐
                            │   SAM UI    │
                            │  (Angular)  │
                            └──────┬──────┘
                                   │ REST/JSON
                            ┌──────▼──────┐          ┌─────────────────┐
                            │ SAM Server  ├──────────►│   PostgreSQL    │
                            │  (Quarkus)  │          └─────────────────┘
                            └──────┬──────┘
                                   │
                    ┌──────────────┼──────────────┐
                    │              │              │
             ┌──────▼──────┐ ┌────▼────┐  ┌──────▼──────┐
             │   Storage   │ │   LLM   │  │     CLI     │
             │ (Local/S3)  │ │Provider │  │  (PicoCLI)  │
             └─────────────┘ └─────────┘  └─────────────┘
```

**External interfaces**

| System | Direction | Purpose |
|--------|-----------|---------|
| PostgreSQL | outbound | Primary data store; provides pg_trgm and phonetic search |
| Local filesystem / AWS S3 | outbound | Document (file) storage |
| LLM provider (Ollama / OpenAI / Vertex AI) | outbound | AI-based document classification and enrichment |
| CLI (PicoCLI) | inbound | Batch import of sheet music data |

SAM has no integrations with rights management systems (GEMA), publishing platforms, or
external music catalogues — identifiers (ISWC, GEMA work number) are stored as plain
strings for reference only.

---

## 4. Use Cases

### S1 – Archive Manager

#### UC-N1: Add a new sheet to the archive

**Actor:** Music librarian  
**Goal:** Register a newly acquired piece so it is findable in SAM  
**Precondition:** Physical score has been received

**Main flow**
1. Music librarian navigates to *Sheets → New sheet*.
2. Enters title, composer, genre, and other known metadata.
3. Optionally uploads the score or individual parts as digital files.
4. Saves. SAM creates the sheet and assigns a deduplication fingerprint.

**Alternative — AI-assisted via upload**
1. Music librarian uploads the PDF to the staging area (*Uploads*).
2. Triggers *Classify*. SAM extracts text or uses vision AI to detect metadata.
3. Reviews the pre-filled form; adjusts if needed.
4. Confirms *Apply*. SAM creates sheet, instrumentation, and links the document.

---

#### UC-N2: Record physical location of a part

**Actor:** Music librarian  
**Goal:** Make a printed part locatable without a manual search  
**Precondition:** Sheet and instrumentation exist in SAM

**Main flow**
1. Opens the sheet detail, navigates to *Instrumentations*.
2. Opens edit dialog for the relevant instrumentation.
3. Enters the archive location (e.g. `Cabinet A / Shelf 3 / Folder 12`).
4. Sets condition to `GOOD` (or the appropriate value).
5. Saves.

---

#### UC-N3: Mark a physical copy as damaged or lost

**Actor:** Music librarian  
**Goal:** Record the actual state of a physical copy so others are not surprised  
**Precondition:** Instrumentation with a recorded physical location exists

**Main flow**
1. Opens the instrumentation edit dialog.
2. Changes *Condition* from `GOOD` to `DAMAGED` or `LOST`.
3. Optionally adds a note explaining the circumstances.
4. Saves.

**Outcome:** The Dirigent and Musiker can see the condition before rehearsal and plan
accordingly.

---

#### UC-N4: Digitise a physical part

**Actor:** Music librarian  
**Goal:** Make a printed part available digitally for download and practice  
**Precondition:** Physical part exists; scanner is available

**Main flow**
1. Scans the part to PDF.
2. Uploads to the staging area.
3. Uses *Classify* to let the AI identify the piece and instrument.
4. Confirms and *Apply* — SAM links the file to the correct instrumentation.

---

#### UC-N5: Find a specific part in the physical archive

**Actor:** Music librarian  
**Goal:** Locate a printed part without searching through all folders  
**Precondition:** Physical location was previously recorded (UC-N2)

**Main flow**
1. Searches for the piece by title or composer.
2. Opens the sheet detail → *Instrumentations* tab.
3. Reads the *Archive Location* column for the relevant instrument.
4. Retrieves the part from the indicated location.

---

### S2 – Conductor

#### UC-D1: Check if a piece is playable by the ensemble

**Actor:** Dirigent  
**Goal:** Know whether the ensemble has all required parts before scheduling a rehearsal  
**Precondition:** Ensemble is defined in SAM with voice definitions

**Main flow**
1. Finds the sheet via search or browsing.
2. Opens the sheet detail.
3. Reads the coverage badge for the relevant ensemble
   (`COMPLETE` / `PLAYABLE` / `INCOMPLETE`).
4. If incomplete: opens the *Coverage* tab to see which voices are missing.

**Alternative — full list overview**
1. Opens *Sheets* with the ensemble coverage filter active.
2. Scans badges in the list to quickly identify playable pieces.

---

#### UC-D2: Build a setlist for a concert

**Actor:** Dirigent  
**Goal:** Compose an ordered programme for a specific concert  
**Precondition:** Sheets are in the archive; coverage has been evaluated

**Main flow**
1. Opens *Collections → New collection*.
2. Sets type to `SETLIST`, enters the concert name and date.
3. Searches for pieces by genre, coverage status, or title.
4. Adds suitable pieces to the setlist, assigning position labels.
5. Reviews the assembled programme.

---

#### UC-D3: Identify gaps in the repertoire

**Actor:** Dirigent  
**Goal:** Know which pieces are *almost* playable and what specific parts are missing  
**Precondition:** Coverage snapshots have been computed for the ensemble

**Main flow**
1. Opens *Sheets*, filters by ensemble and coverage status `INCOMPLETE`.
2. Reviews the list of incomplete pieces.
3. For each priority piece, opens the *Coverage* tab to see which voices are missing.
4. Decides whether to commission new arrangements or acquire missing parts.

---

### S3 – Musician

#### UC-M1: Find my part before rehearsal

**Actor:** Musiker  
**Goal:** Know where the physical part is located in the archive  
**Precondition:** Part location has been recorded by Music librarian

**Main flow**
1. Searches for the piece by title.
2. Opens the sheet detail → *Instrumentations* tab.
3. Finds the row for their instrument.
4. Reads the *Archive Location* value.
5. Retrieves the part from the given location.

---

#### UC-M2: Download a part for home practice

**Actor:** Musiker  
**Goal:** Get a digital copy of their part to practise at home  
**Precondition:** Digital file has been attached to the instrumentation

**Main flow**
1. Opens the sheet detail → *Instrumentations* tab.
2. Expands the row for their instrument to see attached documents.
3. Downloads the relevant file (e.g. PDF of the part).

---

### S3 – Musician (authenticated)

#### UC-M3: Browse the ensemble's full repertoire

**Actor:** Musiker (S3a — authenticated)
**Goal:** Get an overview of all pieces the ensemble has in the archive

**Main flow**
1. Logs in to SAM.
2. Opens *Sheets*, optionally filtered by their ensemble.
3. Browses the list; coverage badges show playability at a glance.
4. Opens a sheet to see instrumentation details and download their part.

---

#### UC-M4: Access a shared link without an account

**Actor:** Musiker (S3b — guest musician)
**Goal:** Download a part received via a shared link before rehearsal

**Main flow** *(planned — not yet implemented)*
1. Receives a URL from the Music librarian (e.g. via WhatsApp group).
2. Opens the link — SAM shows the sheet detail in read-only mode, no login required.
3. Downloads their part directly.

---

### S5 – Guest

#### UC-G1: View a shared setlist

**Actor:** Guest (S5)
**Goal:** See tonight's concert programme without needing an account

**Main flow** *(planned — not yet implemented)*
1. Receives a public link to a setlist collection.
2. Opens the link — SAM shows the ordered programme in read-only mode.
3. Can click individual pieces to read basic metadata (title, composer, duration).

---

#### UC-G2: Download a shared score or part

**Actor:** Guest (S5)
**Goal:** Access a document that the ensemble has made publicly available

**Main flow** *(planned — not yet implemented)*
1. Navigates to a public sheet or document URL.
2. SAM confirms the document has been marked as publicly accessible.
3. Downloads the file.

---

### S4 – Administrator

#### UC-A1: Add a new instrument to the catalogue

**Actor:** Administrator  
**Goal:** Make a new instrument available for instrumentation and ensemble voice definitions  
**Precondition:** Instrument does not yet exist in SAM

**Main flow**
1. Opens *Instruments → New instrument*.
2. Enters the canonical name, display name, and transposition.
3. Saves. The instrument is now selectable in instrumentations and voice options.

---

#### UC-A2: Define or update an ensemble

**Actor:** Administrator  
**Goal:** Ensure the ensemble definition reflects the current composition of the band  
**Precondition:** Ensemble exists or is being created for the first time

**Main flow**
1. Opens *Ensembles → [ensemble name] → Voices*.
2. Adds, edits, or removes voices and their instrument options.
3. Sets required flags, weights, min/target counts appropriately.
4. Recomputes coverage snapshots (`POST /api/ensembles/{id}/coverage/compute`)
   so all sheet badges reflect the updated ensemble.

---

#### UC-A3: Recompute coverage after archive changes

**Actor:** Administrator  
**Goal:** Keep coverage badges accurate after significant changes  
**Trigger:** Bulk import of new sheets, instrument catalogue changes, or voice definition changes

**Main flow**
1. Opens the ensemble detail.
2. Triggers *Recompute coverage*.
3. Waits for completion. All coverage snapshots are updated.

> **Note:** Coverage snapshots are not invalidated automatically on sheet changes.
> Manual recomputation is required after bulk updates.

---

## 5. Key Flows

### Flow 1 – AI-assisted archival of a new physical score

This is the most complete end-to-end flow and the main workflow for the Music librarian.

```
Music librarian                     SAM UI                     SAM Server / LLM
    │                            │                               │
    │── scans score to PDF ─────►│                               │
    │── uploads to staging area ►│── POST /api/documents ───────►│
    │                            │◄── document stored (unlinked) ─│
    │── clicks "Classify" ──────►│── POST /documents/{id}/classify►│
    │                            │                         extract text (PDFBox)
    │                            │                         or vision fallback (LLM)
    │                            │                         match musicians, instruments
    │                            │◄── SheetClassification ────────│
    │◄── review form pre-filled ─│                               │
    │                            │                               │
    │── adjusts, confirms ──────►│── POST /documents/{id}/apply ─►│
    │                            │                         create/resolve Sheet
    │                            │                         create Instrumentation
    │                            │                         link Attachment
    │                            │◄── ClassificationApplyResult ──│
    │◄── sheet detail opens ─────│                               │
    │                            │                               │
    │── opens instrumentation ──►│                               │
    │── enters physical location►│── PUT /instrumentations/{id} ─►│
    │── sets condition = GOOD ──►│                               │
    │◄── saved ──────────────────│                               │
```

---

### Flow 2 – Concert programme check (Dirigent)

```
Dirigent                      SAM UI                     SAM Server
    │                            │                             │
    │── opens Sheets list ──────►│── GET /api/sheets?ensemble=x►│
    │                            │                   return sheets + snapshot badges
    │◄── list with COMPLETE /    │                             │
    │    PLAYABLE / INCOMPLETE   │                             │
    │    badges per piece ───────│                             │
    │                            │                             │
    │── clicks incomplete piece ►│── GET /sheets/{id}/coverage►│
    │                            │                   live evaluation
    │◄── coverage detail:        │                             │
    │    which voices missing    │                             │
    │    score breakdown ────────│                             │
    │                            │                             │
    │── creates setlist ────────►│── POST /api/sheet-collections►│
    │── adds pieces ────────────►│── POST /collections/{id}/sheets►│
    │◄── setlist ready ──────────│                             │
```

---

### Flow 3 – Musician retrieves their part

```
Musiker                       SAM UI
    │                            │
    │── searches by title ──────►│── GET /api/sheets?q=...
    │◄── results ────────────────│
    │── opens sheet detail ─────►│
    │── opens Instrumentations ─►│
    │   tab                      │
    │◄── table shows:            │
    │    Instrument | Part Label  │
    │    | Archive Location       │
    │    | Condition              │
    │                            │
    │── expands row for their    │
    │   instrument ─────────────►│
    │◄── attached files listed ──│
    │── downloads part PDF ─────►│── GET /api/documents/{id}
    │◄── file download ──────────│
```

---

## 6. Access Control Model

> **Status: planned — not yet implemented.**
> SAM currently runs without authentication. This section captures the intended model
> to guide future implementation decisions.

---

### 6.1 Current state

All routes and API endpoints are publicly accessible with no authentication. SAM is
effectively a single-user, trusted-network application. This is acceptable for an initial
deployment on a private server but does not scale to multi-user or internet-facing use.

---

### 6.2 Planned role model

| Role | Maps to | Read access | Write access | Scope |
|------|---------|-------------|--------------|-------|
| `ADMIN` | S4 Administrator | Everything | Everything | Global |
| `MANAGER` | S1 Music librarian | Everything | Sheets, instrumentations, documents | Global |
| `CONDUCTOR` | S2 Dirigent | Everything | Collections / setlists only | Global |
| `MUSICIAN` | S3a Musician | Ensemble repertoire + own parts | None | Per ensemble |
| `GUEST` | S5 Guest | Explicitly published content only | None | Per item |

The `MANAGER` and `CONDUCTOR` distinction allows the Music librarian to manage the archive
without accidentally restructuring ensemble definitions, and the Dirigent to plan
concerts without touching the archive.

---

### 6.3 Content visibility scoping

Three options are under consideration for how "selected content" is defined for guests
and musicians. These are not mutually exclusive.

**Option A — Per-sheet visibility flag**

Each sheet has a `visibility` field: `PRIVATE` (default) · `INTERNAL` (authenticated
users) · `PUBLIC` (guests and unauthenticated access).

- Simple to implement and understand.
- Music librarian sets visibility per sheet when archiving.
- Downside: coarse-grained — all or nothing per sheet.

**Option B — Collection-based sharing**

A collection (folder or setlist) can be marked as shared. All sheets within it become
visible to the target audience (guests or musicians) for the duration of the sharing.

- Natural fit for setlists: "share tonight's concert programme".
- Does not expose the full archive — only what is explicitly curated.
- More flexible than per-sheet flags for time-limited sharing.

**Option C — Ensemble-based scoping for musicians**

An authenticated `MUSICIAN` user is linked to one or more ensembles. They see all sheets
for which a coverage snapshot exists for their ensemble — i.e. pieces their band plays.

- No manual curation needed; driven by the ensemble model.
- Requires that musician user accounts are linked to the existing `Musician` entity.
- Does not cover guest access (still needs Option A or B).

**Recommendation:** Implement Option C for authenticated musicians (low curation overhead)
and Option B for guest access (explicit, time-limited sharing). Option A can be added
later for edge cases.

---

### 6.4 Authentication mechanism

Quarkus has first-class support for OIDC (OpenID Connect) via `quarkus-oidc`. The
natural choices for a self-hosted ensemble application:

| Option | Complexity | Notes |
|--------|-----------|-------|
| Keycloak (self-hosted) | Medium | Full-featured; good fit if already running |
| Auth0 / Okta (SaaS) | Low | No server to maintain; free tier sufficient for small ensembles |
| Quarkus built-in basic auth | Very low | Acceptable for early implementation; no SSO |
| Google / GitHub OAuth | Low | Reduces account management burden; users need a Google/GitHub account |

For a volunteer-run ensemble with non-technical members, a hosted OIDC provider
(Auth0 or Google) is likely the lowest-friction path. Keycloak is worth considering
if the ensemble already runs its own infrastructure.

---

### 6.5 Open design questions

- Should a `Musician` user account be **linked to the existing `Musician` entity**
  (composer/arranger reference)? This would allow showing "your parts" automatically
  but requires the user account to know the musician's instrument(s).
- Should **document-level** visibility be independently configurable, or always inherited
  from the sheet/instrumentation?
- Is **anonymous guest access** (no link, just a public URL) ever desirable, or should
  all guest access require a shared token/link?
- How should **role assignment** work in practice? Self-registration + admin approval,
  or admin-only provisioning?

---

## 7. Glossary

| Term | German | Definition |
|------|--------|------------|
| Sheet music | Notenblatt / Notensatz | A single piece of music in the archive, including all its parts |
| Instrumentation | Instrumentierung / Stimme | One instrument part within a sheet music entry |
| Part label | Stimmenbezeichnung | Distinguishes parts of the same instrument (e.g. "1st Trumpet") |
| Archive location | Archivort | Physical storage location of a printed copy (cabinet, shelf, folder) |
| Physical condition | Zustand | State of a printed copy: Good, Worn, Damaged, or Lost |
| Ensemble | Ensemble / Kapelle | A configured group of voices representing a band or orchestra |
| Voice | Stimmengruppe | An instrument group in an ensemble (e.g. "Tenorhorn", "Tuba") |
| Voice option | Stimmoption | An instrument that can fill a voice (Primary, Alternate, Fallback) |
| Coverage | Abdeckung | Degree to which a sheet music entry has parts for all ensemble voices |
| Coverage status | Abdeckungsstatus | COMPLETE / PLAYABLE / INCOMPLETE — playability classification |
| Coverage snapshot | Abdeckungs-Snapshot | Precomputed coverage result for one ensemble × sheet pair |
| Notenwart | — | Archive manager; responsible for maintaining the sheet music archive |
| Dirigent | — | Conductor; plans repertoire and concerts |
| Blaskapelle | — | Brass/wind band — the typical ensemble type SAM is designed for |
| Attachment | Anhang | A digital file linked to a sheet or instrumentation |
| Staging area | Eingangskorb | Unlinked document pool where uploaded files wait before being assigned |
| Fingerprint | Fingerabdruck | Content-based hash used to detect duplicate sheet music entries |
| ISWC | — | International Standard Musical Work Code — global piece identifier |
| GEMA | — | German rights management society; GEMA work number identifies registered works |
