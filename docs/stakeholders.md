# SAM – Stakeholders, Use Cases & Flows

**S**heet music **A**rchiving & **M**anagement

This document describes who uses SAM, what they need from it, and how the key workflows
look in practice. It complements the [architecture docs](README.md#architecture)
(technical structure) and the [feature reference](features/README.md) with a
human-centred perspective.

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

> **Role overlap:** In smaller ensembles the Music librarian and Conductor are frequently
> the same person. Use cases for both roles should be considered when designing workflows for
> that combined user.

> **Current state:** SAM implements authentication via OIDC (self-hosted Keycloak 26).
> Two realm roles exist today — `admin` and `music_librarian` — both granted full write
> access across the archive; any other authenticated user has read-only access to
> everything (there is no separate `CONDUCTOR` or `MUSICIAN` role at the API level — see
> [Section 6](#6-access-control-model)). The personalised experiences described for S2
> and S3 below (coverage-aware browsing, "My Parts") are additive views built on top of
> that flat read access, not access *restrictions* — any authenticated user can still
> browse the full archive. Guest (S5) access is implemented separately via resource-scoped
> share links, not a login role. The Angular route guard only checks "is logged in," not
> role, so admin-only nav entries are visible to all authenticated users even though the
> underlying write calls are rejected server-side for non-privileged users.

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

The Conductor decides what the ensemble plays. They need to know not just whether a piece
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

The Musician needs their specific part — either physically from the archive folder or as a
digital file for home practice. Their interaction with SAM is typically read-only and
goal-directed: find the right file, download it, done.

There are two sub-variants with different access expectations:

- **S3a — Authenticated musician:** A registered ensemble member with a personal login.
  Can browse and download from the full archive like any authenticated user (there is no
  per-ensemble read restriction today — see [Section 6](#6-access-control-model)). Their
  user account may be linked to an existing `Musician` entity in the data model, which
  unlocks the personalised My Parts view (UC-M5).
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
- Sheet search (full archive — not scoped per ensemble; see [Section 6](#6-access-control-model))
- My Parts (UC-M5) — personalised, instrument-scoped view for linked accounts
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
- Keycloak user lookup (`AdminUsersResource`, admin-only) and linking a `Musician` record
  to a login (`linkUser` / `unlinkUser`, admin-only) — the only two genuinely
  admin-exclusive actions in the system today
- Content visibility / sharing settings — share links are implemented (see S5); persistent
  per-sheet/collection visibility flags are still *(planned)*
- *(stub)* `/admin/configuration` route exists but currently renders only a page title —
  no settings are implemented yet

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
- Public share link for a collection (`/public/share/{token}`) — renders programme order, titles, composers, durations
- Public share link for a single sheet instrumentation — renders instrument, part label, archive location, condition
- Document download via the shared link, where attachments exist on the shared resource

**By design:** sharing is per-resource and creator-initiated — the link must be generated
and distributed by an authenticated user (see UC-N6). Open/anonymous browsing of the
archive (no token, no link) was deliberately deferred in favour of explicit,
resource-scoped tokens — see [roadmap open question #2](roadmap.md#8-open-questions).

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
  │  S1 Archive Mgr  │    │  S2 Conductor    │    │  S3 Musician     │
  │  (Notenwart)     │    │  (Dirigent)      │    │  (Musiker)       │
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

**Outcome:** The Conductor and Musician can see the condition before rehearsal and plan
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

**Actor:** Conductor  
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

**Actor:** Conductor  
**Goal:** Compose an ordered programme for a specific concert  
**Precondition:** Sheets are in the archive; coverage has been evaluated

**Main flow**
1. Opens *Collections → New collection*.
2. Sets type to `SETLIST`, enters the concert name and date.
3. Searches for pieces by genre, coverage status, or title.
4. Adds suitable pieces to the setlist, assigning position labels.
5. Reviews the assembled programme.

**Note:** creating/editing a collection currently requires the `music_librarian` or
`admin` role — a Conductor without one of those roles cannot perform this use case today.
See the `CONDUCTOR` composite-role discussion in [Section 6.2.2](#622-target--composite-roles-mapped-to-use-cases).

---

#### UC-D3: Identify gaps in the repertoire

**Actor:** Conductor  
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

**Actor:** Musician  
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

**Actor:** Musician  
**Goal:** Get a digital copy of their part to practise at home  
**Precondition:** Digital file has been attached to the instrumentation

**Main flow**
1. Opens the sheet detail → *Instrumentations* tab.
2. Expands the row for their instrument to see attached documents.
3. Downloads the relevant file (e.g. PDF of the part).

---

### S3 – Musician (authenticated)

#### UC-M3: Browse the full archive

**Actor:** Musician (S3a — authenticated)
**Goal:** Get an overview of all pieces in the archive

**Main flow**
1. Logs in to SAM.
2. Opens *Sheets*.
3. Browses the list; coverage badges show playability at a glance.
4. Opens a sheet to see instrumentation details and download their part.

**Note:** any authenticated user can browse the entire archive today — there is no
per-ensemble read restriction. "Their ensemble's repertoire" is not yet a scoped view;
see UC-M5 for the one personalised view that does exist (My Parts, scoped by instrument).

---

#### UC-M4: Access a shared link without an account

**Actor:** Musician (S3b — guest musician)
**Goal:** Download a part received via a shared link before rehearsal

**Main flow**
1. Receives a URL from the Music librarian (e.g. via WhatsApp group) — created via
   *Shares → New share* for the relevant sheet instrumentation (see UC-N6).
2. Opens the link (`/public/share/{token}`) — SAM shows the instrumentation detail
   (instrument, part label, archive location, condition) in read-only mode, no login
   required.
3. Downloads their part directly.

**Note:** the link is scoped to one instrumentation, not the whole sheet or archive. An
expired or revoked token shows an error instead of the resource.

---

#### UC-M5: View My Parts

**Actor:** Musician (S3a — authenticated)
**Goal:** See only the sheets that contain a part for an instrument they actually play,
without manually filtering the full archive

**Precondition:** An admin has linked the musician's login to their `Musician` record
(`linkUser`), and that `Musician` has at least one `EnsembleMembership` with an instrument
assigned

**Main flow**
1. Logs in to SAM, opens */my-parts* ("Meine Stimmen").
2. SAM resolves the logged-in user to a `Musician`, collects every instrument from their
   ensemble membership(s) (across all ensembles, doubling instruments included), and
   returns sheets containing at least one matching instrumentation.
3. Each row shows the sheet plus only the matching instrumentation(s) as chips.

**Empty-state cases:** no `Musician` linked to this login; musician on the roster with no
instrument assigned (e.g. conductor-only); or no sheets match their instrument(s) — each
shows a distinct empty state with a hint to contact the Music librarian.

---

### S5 – Guest

#### UC-G1: View a shared setlist

**Actor:** Guest (S5)
**Goal:** See tonight's concert programme without needing an account

**Main flow**
1. Receives a public link to a setlist collection, created via *Shares → New share*
   (resource type `COLLECTION`) by the Music librarian or Conductor.
2. Opens the link (`/public/share/{token}`) — SAM shows the ordered programme in
   read-only mode: position, title, composer, duration.
3. Can download attached documents for entries that have them.

---

#### UC-G2: Download a shared score or part

**Actor:** Guest (S5)
**Goal:** Access a document that the ensemble has made publicly available

**Main flow**
1. Navigates to a public share URL for a sheet instrumentation
   (resource type `SHEET_INSTRUMENTATION`).
2. SAM validates the token (`GET /public/share/{token}`) — confirms it is active
   (not expired or revoked) — and returns the resource.
3. Downloads the attached file.

---

#### UC-N6: Create a share link

**Actor:** Any authenticated user (commonly S1 Music librarian or S2 Conductor)
**Goal:** Give someone without a SAM account access to one specific sheet instrumentation
or collection

**Main flow**
1. Opens *Shares → New share*.
2. Picks the resource type (`SHEET_INSTRUMENTATION` or `COLLECTION`) and the specific
   resource.
3. Optionally sets an expiry date.
4. Saves. SAM generates a token and shows the public URL — *copy link* puts it on the
   clipboard for sharing via WhatsApp, email, etc.
5. Can *revoke* the link at any time from the *Shares* list, immediately invalidating it.

**Note:** share creation is not role-gated — any authenticated user can create and manage
their own share tokens (`SharesResourceImpl` is scoped by creator, not by
`music_librarian`/`admin`). Every link is resource-scoped to exactly one sheet
instrumentation or collection, never the whole archive.

---

#### UC-N7: Enrich an existing sheet's metadata with AI

**Actor:** Music librarian
**Goal:** Fill in missing metadata on a sheet that's already archived, without re-reading
the original document

**Precondition:** Sheet exists with at least its core fields (title, composer, etc.)

**Main flow**
1. Opens the sheet detail, clicks *Enrich with AI* (sparkles button).
2. SAM analyses the known metadata and suggests: tags, and — only for fields that are
   currently empty — style, difficulty level, year of composition, additional notes.
3. Reviews suggestions; each is a pre-checked, individually deselectable checkbox (tags
   as chips).
4. Clicks *Apply selected* — SAM saves the chosen fields and reloads the sheet.

**Note:** unlike classification (UC-N1 alternative flow), this requires no document —
it reasons purely from existing metadata, and never overwrites a field the user has
already filled in.

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

#### UC-A4: Review access and activity history

**Actor:** Administrator (or any authenticated user — see note)
**Goal:** Understand who accessed or exported a document/sheet/collection, and when

**Main flow**
1. Opens */admin/event-logs*.
2. Filters by event type (multi-select: downloads, exports, classification, etc.), user
   ID, or entity type.
3. Reviews matching entries — each shows timestamp, actor (`username`, or "via share
   link" with the token ID for unauthenticated access), event type, and target entity.

**Note:** despite living under the `/admin/` route, `EventLogResourceImpl` carries no
`@RolesAllowed` restriction — any authenticated user can call `GET /api/event-logs`
directly today. Read-only and contains no sensitive data beyond `userId`/`username`, but
worth deciding deliberately (see the composite-role discussion in
[Section 6](#6-access-control-model)) rather than leaving it as an accident of routing.

---

## 5. Key Flows

Full sequence diagrams for these flows live in
[`docs/architecture/runtime/`](architecture/runtime/README.md) (one `.puml` file per
flow, viewable in any PlantUML renderer or IDE plugin — matching the
one-diagram-per-file convention already used by `docs/architecture/architecture.puml`
and `src/site/resources/*.puml` elsewhere in this repo). This section gives the short
version of each.

### Flow 1 – AI-assisted archival of a new physical score

This is the most complete end-to-end flow and the main workflow for the Music librarian:
scan → upload to staging → *Classify* (text extraction, vision fallback for scanned
pages) → review pre-filled form → *Apply* (creates/resolves Sheet, Instrumentation,
Attachment) → record physical location and condition on the resulting instrumentation.

→ [flow-1-ai-assisted-archival.puml](architecture/runtime/flow-1-ai-assisted-archival.puml)

---

### Flow 2 – Concert programme check (Conductor)

Opens the Sheets list scoped to an ensemble, reads coverage badges (`COMPLETE` /
`PLAYABLE` / `INCOMPLETE`), drills into an incomplete piece for the per-voice breakdown,
then creates a setlist and adds suitable pieces to it.

→ [flow-2-concert-programme-check.puml](architecture/runtime/flow-2-concert-programme-check.puml)

---

### Flow 3 – Musician retrieves their part

Searches by title, opens the sheet's *Instrumentations* tab, finds the row for their
instrument (archive location, condition), expands it for attached files, and downloads
the part.

→ [flow-3-musician-retrieves-part.puml](architecture/runtime/flow-3-musician-retrieves-part.puml)

---

### Flow 4 – Share creation and guest access

Music librarian creates a resource-scoped share token (`POST /api/shares`) for one
instrumentation or collection, copies the public URL, and distributes it. The guest opens
`/public/share/{token}` with no login; the server validates the token isn't expired or
revoked, renders the resource, and logs the access with `shareTokenId` set instead of a
`userId`.

→ [flow-4-share-creation-guest-access.puml](architecture/runtime/flow-4-share-creation-guest-access.puml)

---

## 6. Access Control Model

> **Status: implemented (Phase 1–3 of RBAC).** Authentication, role enforcement, and
> resource-scoped sharing are live. What's documented below as "target" is not — it's a
> proposal for closing the remaining gaps, kept separate from "today" so the two are never
> confused.

---

### 6.1 Current state

Authentication is OIDC via a self-hosted Keycloak 26 (`docker-compose.keycloak.yml` for
dev, realm export at `keycloak/sam-realm.json`). `@Authenticated` is enforced at the class
level on every `*ResourceImpl`; write methods additionally carry
`@RolesAllowed({"music_librarian", "admin"})`. `CurrentUserService` exposes the JWT
subject, realm roles, and `ensemble:{UUID}` group membership, but nothing currently reads
the group claim to scope access — it's wired but unused. The Angular route guard
(`authGuard`) checks authentication only, not role, so every authenticated user sees every
nav entry (including `/admin/*`); unauthorized write attempts fail server-side with 403
rather than being hidden client-side.

Two findings worth flagging explicitly (surfaced while writing this section, not
yet decided on):
- **No `CONDUCTOR` write tier exists.** The originally planned model gave the Conductor
  write access to collections/setlists only. In the current code, `SheetCollectionsResourceImpl`
  and `CollectionItemsResourceImpl` require the same `music_librarian`/`admin` roles as
  everything else — a Conductor who isn't also a librarian cannot create a setlist today.
- **`EventLogResourceImpl` has no `@RolesAllowed` at all** — any authenticated user can
  call `GET /api/event-logs` directly, despite the Angular route living under `/admin/`.
  Low risk (read-only, no sensitive data beyond `userId`/`username`), but it's an artifact
  of routing rather than a deliberate access decision.

---

### 6.2 Role matrices

#### 6.2.1 Today — what's actually enforced

| Capability | `admin` | `music_librarian` | authenticated, no special role | unauthenticated + valid share token |
|---|---|---|---|---|
| Browse/search/read archive (sheets, collections, instrumentations, coverage) | ✅ | ✅ | ✅ | ➖ (only the one shared resource) |
| Create/edit/delete sheets, instrumentations, documents; run AI classify/apply/enrich | ✅ | ✅ | ❌ | ❌ |
| Create/edit/delete collections & setlist items | ✅ | ✅ | ❌ | ❌ |
| Create/edit/delete musicians & instruments | ✅ | ✅ | ❌ | ❌ |
| Create/edit/delete ensembles, voices, voice options, members; compute coverage | ✅ | ✅ | ❌ | ❌ |
| Link/unlink a `Musician` to a login; search Keycloak users | ✅ | ❌ | ❌ | ❌ |
| Create/list/revoke **own** share tokens | ✅ | ✅ | ✅ | ❌ |
| View My Parts (own personalised view, if linked) | ✅ | ✅ | ✅ | ❌ |
| View event log (`GET /api/event-logs`) | ✅ | ✅ | ✅ *(see finding above)* | ❌ |
| Access exactly the resource named by a share token | n/a | n/a | n/a | ✅ |

The practical read model today is flat: **any login = read everything.** The only
differentiators are (a) the two write roles, both equally powerful except for user
linking, and (b) the unauthenticated, token-scoped guest path.

#### 6.2.2 Target — composite roles mapped to use cases

This reframes the same capabilities as named composite roles, so each persona maps to
one role instead of a list of individual checks. None of this requires a rewrite — it's
additive on top of 6.2.1.

| Composite role | Maps to | Built from | New work required |
|---|---|---|---|
| `ARCHIVE_ADMIN` | S4 Administrator | Everything `music_librarian` has, plus user/account linking | None — already `admin` today |
| `ARCHIVE_MANAGER` | S1 Music librarian | Write: sheets, instrumentations, documents, classification, musicians, instruments, ensembles/voices/members. Read: everything | None — already `music_librarian` today |
| `CONDUCTOR` | S2 Conductor | Read: everything. Write: collections/setlist items + trigger coverage compute only | New 3rd Keycloak role; narrow `@RolesAllowed` on `SheetCollectionsResourceImpl`, `CollectionItemsResourceImpl`, and the ensemble coverage-compute endpoint to accept it alongside `music_librarian`/`admin` |
| `MUSICIAN` | S3a Authenticated musician | Read: everything (unchanged). Own: My Parts, own share tokens | Formalizing this as an explicit role buys nothing functionally yet (it's already the default for any login) — only worth adding once read access needs to be *restricted* (e.g. per-ensemble), per Option C below |
| `GUEST` | S5 / S3b | Read: exactly one resource per token, until expiry/revocation | None — already implemented as token-based access, deliberately not a Keycloak role |

**Recommendation:** don't add `CONDUCTOR` speculatively. Add it only when a real Conductor
needs to build setlists without also holding `music_librarian` — until then, give
conductors the `music_librarian` role as a practical stand-in (it's a superset, just
broader than strictly necessary). `MUSICIAN` as a distinct role is even lower priority
since it changes nothing observable today.

---

### 6.3 Content visibility scoping

Three options were considered for how "selected content" is defined for guests and
musicians. Status, updated against what shipped:

**Option A — Per-sheet visibility flag** — *(still planned)*. Tracked as
`SheetCollection.visibility` in the [roadmap](roadmap.md#5-access-control--sharing) ("Collection visibility & cover");
no equivalent field exists on `SheetMusicEntity` itself yet.

**Option B — Collection-based / resource-scoped sharing** — *(implemented, in a
different shape than proposed)*. Rather than a persistent "shared" flag on a collection,
SAM implemented **resource-scoped tokens** (`shares` table): one token = one collection or
one sheet instrumentation, with optional expiry and explicit revocation. More flexible
than a boolean flag (multiple links, independent expiry) at the cost of the librarian
needing to actively generate and distribute each link (see UC-N6).

**Option C — Ensemble-based scoping for musicians** — *(not implemented as a read
restriction; partially realized as a different, additive feature)*. The plan was to
*restrict* a `MUSICIAN` role to their ensemble's repertoire. What exists instead is **My
Parts** (UC-M5): an *instrument-based*, *additive* personal view layered on top of
unrestricted read access — it doesn't gate what a musician *can* see, only what's
highlighted for them. Genuine ensemble-scoped read restriction is still open; the unused
`ensemble:{UUID}` group claim on the JWT (see 6.1) is the most likely foundation for it.

---

### 6.4 Authentication mechanism

**Resolved and implemented:** self-hosted Keycloak 26, via `quarkus-oidc` on the backend
and `angular-auth-oidc-client` on the frontend. Dev environment: realm auto-imported from
`keycloak/sam-realm.json` via `docker compose -f docker-compose.keycloak.yml up` (port
8180). Production config (`OidcConfigResource`) reads `OIDC_SERVER_URL` /
`OIDC_CLIENT_ID` from env vars — no rebuild needed to point at a different Keycloak.

---

### 6.5 Open design questions

Most of the original open questions here are now resolved — see
[roadmap Section 8](roadmap.md#8-open-questions) for the authoritative, up-to-date list (it already tracks resolution status and
is updated alongside feature work, so this doc intentionally doesn't duplicate it).
Genuinely new questions surfaced while writing this section:

- Should `CONDUCTOR` be added as a real Keycloak role (6.2.2), or is routing conductors
  through `music_librarian` an acceptable permanent simplification for ensembles this
  size?
- Should `EventLogResourceImpl` get an explicit `@RolesAllowed` (matching its `/admin/`
  route), or is "any authenticated user can read the event log" an acceptable, deliberate
  choice worth just documenting as such?
- Now that `CurrentUserService` already resolves `ensemble:{UUID}` group membership, is
  there an actual demand for restricting general archive *read* access per ensemble
  (true Option C), or does "read everything, personalize via My Parts" remain sufficient?

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
| Archive manager | Notenwart | Responsible for maintaining the sheet music archive |
| Conductor | Dirigent | Plans repertoire and concerts |
| Brass band | Blaskapelle | The typical ensemble type SAM is designed for |
| Attachment | Anhang | A digital file linked to a sheet or instrumentation |
| Staging area | Eingangskorb | Unlinked document pool where uploaded files wait before being assigned |
| Fingerprint | Fingerabdruck | Content-based hash used to detect duplicate sheet music entries |
| ISWC | — | International Standard Musical Work Code — global piece identifier |
| GEMA | — | German rights management society; GEMA work number identifies registered works |
| Share token / share link | Freigabe-Link | Resource-scoped, optionally time-limited token granting unauthenticated read access to exactly one sheet instrumentation or collection |
| My Parts | Meine Stimmen | Personalised, read-only view for an authenticated musician showing only sheets containing an instrumentation for an instrument they play |
| Event log | Zugriffsprotokoll | Write-once log of read events (downloads, exports, classification) not captured by the Envers audit trail |
| Composite role | — | A proposed bundle of capabilities mapped to a persona (e.g. `CONDUCTOR`), as distinct from the Keycloak realm roles actually enforced today (`admin`, `music_librarian`) |
