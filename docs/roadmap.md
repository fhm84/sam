# SAM – Roadmap & Ideas

**S**heet music **A**rchiving & **M**anagement

This document collects planned features, ideas under consideration, and open questions.
It is intentionally informal — a living list rather than a project plan.

For the **current feature set** see the [feature reference](features/README.md).
For **stakeholder context and use cases** see [stakeholders](stakeholders.md).
For **technical architecture** see the [architecture docs](README.md#architecture).

Status values: `idea` · `planned` · `in progress` · `done`

Task-level breakdowns for `planned` / `in progress` items live in
[implementation plans](plans/README.md).

---

## Now / Next / Later

The order of work, reviewed 2026-10-03. Everything else in this document is an
unprioritised backlog.

**Now: before go-live** (real musicians will log in, and the event log will hold personal data)

1. [Role-aware access](#full-role-based-access-control-rbac--in-progress): menu and route
   guarding, and scoping read endpoints (event log, musician contact data, share creation)
   → [plan](plans/rbac-role-aware-access.md)
2. [Automatic coverage snapshot invalidation](#automatic-coverage-snapshot-invalidation--planned)
   → [plan](plans/coverage-snapshot-invalidation.md) (needs open question #5)
3. [Event log retention + own-history view](#document-access-log--partial)
   → [plan](plans/event-log-retention.md) (needs open question #9)
4. Instrumentation-count search filter (PR #96, ready to merge)

**Next: everyday use for the music librarian**

1. [Classification form enhancements](#classification-form-enhancements--planned) → [plan](plans/classification-enhancements.md)
2. [Sheets overview filter & bulk actions](#sheets-overview-filter--bulk-actions--planned) → [plan](plans/sheets-overview.md)
3. [Home dashboard](#home-dashboard--planned) → [plan](plans/home-dashboard.md),
   including the gap report from [coverage breakdown enhancements](#coverage-breakdown-enhancements--planned) → [plan](plans/coverage-breakdown.md)

**Later**

- [Performance history](#performance-history--idea), which unblocks the date-range
  [GEMA reporting export](#gema-reporting-export--in-progress)
- [Checkout / lending tracking](#checkout--lending-tracking--idea),
  [QR codes](#qr-codes-on-physical-folders--idea) +
  [mobile quick-lookup view](#mobile-first-quick-lookup-view--idea),
  [thumbnail preview](#thumbnail-preview--idea)
- Attachment kind and versioning → [plan](plans/attachment-metadata.md)
- [Classification queue](#classification-queue--planned) → [plan](plans/classification-queue.md),
  then [aligning the UI with the Claude Design](#align-ui-with-the-improved-claude-design--idea)

**Favourites from the 2026-10 idea round** (not yet scheduled; the other ten ideas from that
round are in their sections too, as new `idea` entries or as extensions of existing ones)

- [Personal concert folder](#personal-concert-folder--idea): one PDF per musician, in programme order
- [Metadata lookup from external catalogues](#metadata-lookup-from-external-catalogues--idea)
- [Mobile part capture](#mobile-part-capture--idea): phone as scanner, straight into classification
- [Search by mood or description](#search-by-mood-or-description--idea): semantic search and "similar pieces"

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

### Mobile part capture — `idea`

Turn a phone into a scanner at the archive cabinet: a mobile-friendly capture page (PWA,
camera access) photographs a part page by page, detects and straightens the page edges,
improves contrast, combines the pages into one PDF, and hands it straight to the existing
[AI classification](features/ai-classification.md) workflow.

That makes digitising the physical archive something any member can help with in spare
minutes, not only whoever sits at the flatbed scanner. Image clean-up can run in the
browser (e.g. OpenCV.js) or server-side next to the existing PDF rendering in
`DocumentUtils`. Pairs well with [QR codes on physical folders](#qr-codes-on-physical-folders--idea):
scan the folder's QR code, then capture its parts.

**Stakeholders:** S1 (music librarian), S3 (Musiker helping with digitisation)
**Effort:** Medium

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

**Extension: availability-aware coverage for a concert date.** The same question,
asked ahead of time: "can we play this setlist on 14 June, when two trumpets and the tuba
are away?" Members record absences (date ranges) themselves, and a setlist with a date
evaluates coverage against the line-up that will actually be there. Pieces that become
unplayable are flagged early enough to swap them out or find a substitute (see
[substitute finder](#substitute-musician-finder--idea)). Needs an absence table on top of
the existing memberships; the coverage engine is reused unchanged.

**Stakeholders:** S2 (Dirigent)
**Effort:** Medium
**Depends on:** Musician–instrument assignment (done); absence model for the concert-date variant

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

**Extension: conductor markings shared to all parts.** Some notes apply to every
player: "cut bars 33–48", "repeat only once", "start at letter C". The conductor enters
them once per setlist entry and they appear as a cover note (or a stamped overlay) on
every part of that piece: in the [personal concert folder](#personal-concert-folder--idea),
in My Parts and on shared links. Today this goes round by word of mouth, and someone
always misses it.

**Stakeholders:** S2 (Dirigent)
**Effort:** Low (the join entity already exists; add a field + UI textarea); the overlay on parts is Medium

---

### Programme flow analysis — `idea`

Show how a setlist flows across its pieces: tempo curve, key sequence, difficulty,
running time and genre mix, as a small chart on the setlist page. Warn about common
programme mistakes: "three marches in a row", "same key three times in a row", "the two
hardest pieces back to back", "8 minutes over the planned length".

The [AI setlist assistant](#ai-setlist-assistant--done) could use the same rules to
suggest a better running order. Uses the tempo, tonality, duration and difficulty fields
that already exist; no new data needed.

**Stakeholders:** S2 (Dirigent)
**Effort:** Low–Medium

---

### Rehearsal planning & readiness — `idea`

Working back from the concert date, spread the remaining rehearsals across the setlist
by difficulty and how ready each piece is. Musicians mark per piece "comfortable" /
"needs practice" (optionally per passage), and the conductor sees a readiness heatmap per
piece and section to decide what to rehearse next.

**Stakeholders:** S2 (Dirigent), S3 (Musiker)
**Effort:** Medium
**Depends on:** Role-aware access (musicians writing their own readiness)

---

### AI setlist assistant — `done`

A tool-grounded AI assistant that helps build a setlist and draft the spoken text between
songs. See [AI Setlist Assistant](features/ai-setlist-assistant.md).

**Implementation note:** Backend (ensemble FK, candidate-retrieval tool, `SetlistAssistant` /
`ProgrammeTextDrafter` AI services, both endpoints, event logging with token usage, draft-text
language passed from the UI's active locale) is done and tested (162 server tests pass). Angular
UI (assistant drawer + draft-text action) is implemented and browser-verified end-to-end with
Playwright, including a real round-trip to OpenAI (blocked only by account credits, not a code
issue). Fixed along the way: a stray open-ended `@babel/core` override in
`ui/src/main/webui/package.json` that let Babel 8 leak in and broke `ng serve` for the whole app
(unrelated to this feature, now pinned to exact 7.29.7), and a PrimeNG `p-table` OnPush
stale-binding bug where the draft-text button's loading state needed to be a signal rather than a
plain property to render correctly.

- **Program builder** — the Dirigent gives free-text goals/constraints ("45 min opener,
  upbeat, avoid two marches in a row") for an existing setlist. An AI service with tool
  access searches the real repertoire (filtered by ensemble coverage status, duration,
  genre, difficulty, tags) and returns ranked suggestions with rationale — real sheet IDs
  only, never invented pieces. Reuses the tool-calling pattern already established by
  `ClassificationAgent`.
- **Programme text drafting** — for a text item between two pieces in the setlist, draft a
  short spoken intro from the neighboring piece's metadata, for the Dirigent to review and
  edit before use.

**Explicitly out of scope:** rehearsal-planning hints for the Dirigent (that's the
deterministic gap-report/recommendation-solver work under "Coverage breakdown
enhancements" above, not an LLM task) and free-form new-song-idea suggestions (no
grounding data source yet — would just be hallucination).

**Stakeholders:** S2 (Dirigent)
**Effort:** Medium
**Depends on:** Ensemble FK on `SheetCollection` (see "Sheet metadata enrichment" above)

---

## 3. Musician-Facing

### Musician profile enrichment — `done`

**Done:** all fields shipped (migration `V1.1.2__MusicianFields.sql`, `MusicianStatus`,
`MusicianRole`, `musician_instruments` junction table) together with the Angular musician
form. See [Musicians](features/musicians.md). The contact fields are currently readable by
every authenticated user; restricting that is part of [role-aware access](plans/rbac-role-aware-access.md).

Added missing fields to the `Musician` entity surfaced in the Claude Design mockup
(`Create Flows (PrimeNG).html`).

Fields added:
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

**Update (verified 2026-09-08):** frontend OIDC is fully wired (`angular-auth-oidc-client`);
the "My Parts" view (`/my-parts`) resolves the real logged-in user's `sub` claim end-to-end,
not a stub. The remaining gap is UI role-awareness, not identity plumbing — see "Full RBAC
remaining work" below.

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

### Personal concert folder — `idea`

The digital counterpart of the part distribution list above. One click on a setlist
creates, for each musician, a single PDF of *their* parts in programme order, with a
table of contents, page numbers and the setlist's
[conductor markings](#rehearsal-notes-per-setlist-entry--idea). Two layouts: a
print-ready booklet (duplex, page-turn friendly) and a tablet version. Delivered in My
Parts, or as a share link for guest players.

Most building blocks exist: My Parts already resolves who plays which instrumentation,
the batch download already merges PDFs, and setlist items have an order. What's new is
per-musician assembly, the TOC/page-number stamping, and handling parts that have no
digital file yet (listed as "physical only" in the TOC).

**Stakeholders:** S1 (music librarian), S3 (Musiker), S3b (Guest musician)
**Effort:** Medium

---

### Reference recordings & practice mode — `idea`

Link one or more reference recordings to a sheet: a YouTube/Spotify link (found via a
search helper) or an uploaded audio file, for example the band's own concert recording.
In My Parts, a musician sees their part next to the recording and can practise along.
A later step could add slow-down playback and loop sections.

Uploaded recordings use the existing storage layer. Recordings of the band's own
performances need a rights check before being shared outside the band.

**Stakeholders:** S3 (Musiker), S2 (Dirigent)
**Effort:** Low (links) to Medium (upload + player)

---

### Substitute musician finder — `idea`

When availability-aware coverage (see
[minimum viable setlist](#minimum-viable-setlist--idea)) shows a gap for a concert,
suggest musicians who could fill it: those with role `GUEST` / `SUBSTITUTE` who play the
missing instrument, from the global `musician_instruments` list. One click sends them a
share link containing only the parts they would play.

**Stakeholders:** S1 (music librarian), S2 (Dirigent), S3b (Guest musician)
**Effort:** Low–Medium
**Depends on:** Availability-aware coverage, [instrument-limited setlist sharing](#instrument-limited-setlist-sharing--idea)

---

## 4. Statistics & Reporting

### Home dashboard — `planned`

A data-driven home page replacing the current static landing page (greeting + navigation
cards, no data). Two tabs, derived from the
Claude Design `Hi-Fi Shell (PrimeNG).html`. Task breakdown: [plan](plans/home-dashboard.md).

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

### Business metrics in Grafana — `idea`

The existing Prometheus/Grafana stack (`monitoring/grafana/dashboards/sam.json`) only
covers ops metrics — HTTP request rate/latency/errors, JVM heap/GC, HikariCP pool, AI
classification duration, LLM token usage. It has no business-entity counts (total
sheets, collections, musicians, ensembles, instruments, voices).

Add Micrometer gauges for these counts (e.g. via a scheduled or on-demand repository
count query) so they're scraped by Prometheus and can be charted in Grafana alongside
the ops panels — distinct from the in-app "Home dashboard" KPIs below, which are
read-in-the-UI rather than an ops/trend view.

**Stakeholders:** S4 (Administrator)
**Effort:** Low (Micrometer gauge registration + a new Grafana panel)

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

### Automatic rights hints — `idea`

Use rights data SAM already stores to give hints, not legal verdicts:

- **Likely public domain:** composer (and arranger) death year + 70 years has passed →
  suggest `rightsStatus = PUBLIC_DOMAIN` for review.
- **Expiring arrangement rights:** `arrangementRightsUntil` within the next N months →
  shown on the home dashboard / in the [notifications digest](#notifications--digest--idea).
- **GEMA prefill:** once performance history exists, prefill the GEMA report from the
  concerts that actually took place, using only sheets marked `gemaReportable = YES`.

**Stakeholders:** S1 (music librarian), S4 (Administrator)
**Effort:** Low (fields exist; a rule service + UI badges)

---

## 5. Access Control & Sharing

### Setlist public page (guest access — minimal) — `done`

**Implementation note:** Implemented as part of the Share Links feature (see below). The
music librarian creates a resource-scoped share token for a collection via
`POST /api/shares`. The resulting public URL (`/share/{token}`) renders the
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

**Done:** Frontend OIDC integration (`angular-auth-oidc-client`) and the "My parts" view
scoped to the logged-in musician's ensemble memberships (`/my-parts`, `GET /api/me/parts`)
— both verified working end-to-end as of 2026-09-08.

**Remaining (Phase 4+) — role-aware UI, verified still missing 2026-09-08:**
- **Route/menu guarding by role** — `app.routes.ts` only gates on "authenticated or not"
  (`authGuard`); `app-menu.ts` has no role filtering at all. Any authenticated user
  currently sees and can navigate to `admin/ensembles`, `admin/instruments`,
  `admin/configuration`, `admin/event-logs` in the menu regardless of role — the backend's
  `@RolesAllowed` still blocks writes, but read-only UI exposure is broader than intended
  for a plain `music_librarian`/musician account.
- **No frontend ensemble/context service** — "ensemble" only exists as a local filter
  dropdown inside the sheets list (`sheets.ts`); there is no shared context (selected
  ensemble/sheet) that other features (menu, dashboards, forms) could read from. This is
  the natural foundation for both route guarding above and use-case-tailored UI below.
- **Use-case-tailored UI** — no differentiation in what's shown/available between
  Musiker, music librarian (Notenwart), Dirigent, and Guest beyond raw role checks; e.g.
  no simplified/guest-appropriate views distinct from the full librarian UI.
- Conductor role surfaced in the UI (currently stored in data model, not yet used for
  access control)

**Stakeholders:** All
**Effort:** High
**Auth provider chosen:** Self-hosted Keycloak 26 (`ensemble:{UUID}` groups for per-ensemble access)

---

### Shared document links — `done`

**Implementation note:** Fully implemented. The `shares` table stores resource-scoped
tokens (one token = one resource: a sheet instrumentation or a collection).
`POST /api/shares` creates a token; `GET /api/public/shares/{token}` is the unauthenticated
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

### Instrument-limited setlist sharing — `idea`

A `COLLECTION` share token currently grants access to the full setlist — the TOC plus
*every* instrument's parts within it. There is no way to share a setlist scoped to
specific instruments only (e.g. "send the trumpet parts for this concert to the guest
trumpeter" without exposing every other section's material).

`ShareType.INSTRUMENTATION` already covers the single-sheet, single-part case, but not
"one instrument, across all pieces in a setlist." Would need either a new share type
(collection + instrument filter) or an `instrumentIds` filter list stored alongside the
existing `COLLECTION` share row and enforced in `ShareService`/`PublicShareResourceImpl`.

**Stakeholders:** S1 (music librarian), S3b (Guest musician)
**Effort:** Low–Medium
**Depends on:** Shared document links (already done)

---

### Collection visibility & cover — `done`

**Done:** shipped in migration `V1.1.3__CollectionFields.sql` with a visibility dropdown and
a cover colour picker in the collection form. **Leftovers:** `visibility` is stored but no read
endpoint enforces it yet (part of [role-aware access](plans/rbac-role-aware-access.md));
`coverImageId` exists, but no upload control sets it yet (tracked in
[attachment metadata](plans/attachment-metadata.md)).

Added missing fields to `SheetCollection` surfaced in the Claude Design mockup
(`Create Flows (PrimeNG).html`).

- **visibility** — enum (`WHOLE_ENSEMBLE` / `ADMINS_ONLY` / `PRIVATE`); meant to control
  who can see the collection (not enforced yet, see above). The design shows a "Whole ensemble" dropdown in the
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
Task breakdown: [plan](plans/coverage-breakdown.md).

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
Claude Design `Classify (PrimeNG).html`. Scope and open questions: [plan](plans/classification-queue.md).

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
complete apply request. Task breakdown: [plan](plans/classification-enhancements.md).

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

### Sheet create wizard — `done`

Three fields shown in the `Create Flows (PrimeNG).html` create wizard were missing from
the data model; all three shipped (migration `V1.1.4__CreateSheetWizardFields.sql`).

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

This is already noted as a known gap in the [coverage concept](architecture/concepts/coverage.md).

**Stakeholders:** S2 (Dirigent), S4 (Administrator)
**Effort:** Medium

---

### Sheet metadata enrichment — `in progress`

Add missing fields to `SheetMusicEntity` and related entities surfaced in the Claude
Design mockup (`Sheet Detail (PrimeNG).html` / `Sheet Detail v2 (PrimeNG).html`).
Remaining attachment work: [plan](plans/attachment-metadata.md).

**Sheet-level fields — done:**
- **rightsStatus** — enum (`UNKNOWN` / `PUBLIC_DOMAIN` / `LICENSED` /
  `PERMITTED_ARCHIVE` / `RESTRICTED` / `NO_DIGITALIZATION`); shipped with two more
  values than originally scoped here (`PERMITTED_ARCHIVE`, `NO_DIGITALIZATION`) to
  cover rental/hire-only material. See [Sheet Music — Rights status](features/sheets.md#rights-status).
- **gemaReportable** — 3-state enum (`UNKNOWN` / `YES` / `NO`); "GEMA-pflichtig" in German. See
  [Sheet Music — GEMA reportable](features/sheets.md#gema-reportable).
- **tempo** (Integer, bpm) — shown in Base Data card.
- **tonality** — fixed enum, full circle of fifths (15 major + 15 minor keys) plus
  `ATONAL`; shipped as a superset of the originally scoped "major/minor keys" note. See
  [Sheet Music — Tonality](features/sheets.md#tonality).
- **arrangementPublisher** + **arrangementRightsUntil** — simple fields for
  "Musikverlag Tirol · until 2080"-style arranger rights; no separate entity.

**Attachment-level fields — still planned:**
- **AttachmentKind** — new enum (`CLEAN` / `MARKED_UP` / `PHOTOCOPY` / `FACSIMILE` /
  `SCORE`) as a separate field alongside the existing `AttachmentType`
- **version** (Integer) + **replacedById** (self-referencing FK) — for v1/v2/v3
  file lineage tracking

**Collection — done:**
- **ensemble FK on SheetCollection** — shipped as part of the AI setlist assistant
  (V1.1.5); scopes coverage-aware suggestions to the linked ensemble. See
  [Collections & Setlists](features/collections.md).

**Deferred:** Duplicate / Merge / Split sheet operations (More menu in v2).

**Stakeholders:** S1 (music librarian), S2 (Dirigent), S4 (Administrator)
**Effort:** Medium

---

### Instrument catalogue enrichment — `done`

**Done:** all fields shipped in migration `V1.1.1__InstrumentFields.sql` (`InstrumentFamily`,
default clef, `instrument_aliases`, catalogue section/position). See [Instruments](features/instruments.md).

Added missing fields to the `Instrument` entity surfaced in the Claude Design mockup
(`Create Flows (PrimeNG).html`).

Fields added:
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

**Implementation note:** Sheet and collection export (ZIP/JSON/CSV) is implemented. The migration use case is additionally covered by the CLI: `export`/`import` plus `export…`/`import…` for instruments, musicians, and ensembles round-trip all metadata into a fresh instance (idempotent, validated — see `cli/README.md`). Full archive export with all document files is a future enhancement.
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
| `SHARE_CREATED` / `SHARE_REVOKED` | Share token created / revoked |
| `SHARE_ACCESSED` | Shared resource accessed via a share token |
| `SETLIST_AI_SUGGESTION_GENERATED` / `SETLIST_AI_TEXT_DRAFTED` | AI setlist assistant used (with token usage) |

The [event log feature page](features/event-log.md) is the authoritative list.

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

### Metadata lookup from external catalogues — `idea`

Fill in title, composer, arranger, publisher, duration and difficulty grade from outside
sources instead of typing them: IMSLP and MusicBrainz/Wikidata for older works and
composer life dates, and publisher catalogues where an API or structured data exists.
Results appear as suggestions in the existing [AI enrichment](features/ai-enrichment.md)
dialog for the librarian to accept field by field, never applied silently.

A lookup tool the AI enrichment can call fits the existing tool-grounded pattern.
Composer death years from Wikidata also feed the
[automatic rights hints](#automatic-rights-hints--idea).

**Stakeholders:** S1 (music librarian)
**Effort:** Medium (per-source adapters; publisher catalogues vary a lot)

---

### Generated parts by transposition (OMR) — `idea`

A common coverage gap is "no Eb horn part, but there is an F horn part", or "no tenor
horn part, but a baritone treble-clef part exists". Optical music recognition (e.g.
Audiveris or oemer) can turn a clean scan into MusicXML, which can then be transposed and
re-engraved (e.g. with Verovio) into the missing part.

The coverage engine already knows substitute relationships; this would turn a
substitute into a real sheet. The [recommendation solver](plans/coverage-breakdown.md)
could then suggest "generating this part would move 12 sheets to PLAYABLE". It fits the
design principle that derivatives (including an OMR tier) are generated on demand from one
master scan.

**Caveats:** OMR quality on old photocopies is mixed, so a generated part needs a review
step before it's used. Only offered when the sheet's `rightsStatus` allows it, since
creating a new part from a copyrighted arrangement is an adaptation.

**Stakeholders:** S1 (music librarian), S2 (Dirigent)
**Effort:** High (research spike first: OMR accuracy on our own scans)

---

### Sheet-music exchange between bands — `idea`

The bigger version of [lending to partner ensembles](#lending-to-partner-ensembles--idea):
an opt-in directory where SAM instances show which pieces they *physically* own, so that
bands in a region can find and borrow sheets from each other instead of buying them
again. Pieces are matched across instances by the existing content fingerprints, not
just by title. Only metadata is shared, never files.

Needs a small central directory service (or a federated protocol) and only pays off when
several bands take part. A long-term vision, not a near-term feature.

**Stakeholders:** S1 (music librarian), partner ensembles
**Effort:** High

---

### SAM MCP server — `idea`

Expose SAM's API as tools for Claude and other AI assistants via the Model Context
Protocol, so that the librarian or conductor can ask, from their usual assistant:
"which marches haven't we played in two years that the current line-up can play?",
"prepare the GEMA list for last weekend", or "add these three pieces to the summer
concert".

API-first design and tool-grounded AI (no invented IDs) are already SAM's pattern, so this
is mostly packaging: a small MCP module reusing the `api` REST client (like the `cli`
module), with the user's own OIDC token so that access control applies unchanged.
Write tools should require confirmation.

**Stakeholders:** S1 (music librarian), S2 (Dirigent)
**Effort:** Low–Medium

---

## 7. UX & Discovery

### Sheets overview filter & bulk actions — `planned`

Extend the sheet list API and Angular UI to match the filter toolbar and bulk actions
shown in the Claude Design `Sheets Overview (PrimeNG).html`. Task breakdown: [plan](plans/sheets-overview.md).

**Missing filter dimensions** (to add to `SheetFilterRequest`):
- Coverage status filter (COMPLETE / PLAYABLE / INCOMPLETE, per ensemble)
- Difficulty level filter (multi-select)
- Duration range filter (min/max)
- Tags filter: multi-select, AND or OR (a single-tag `tag` filter already exists)
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

### Server-side musician search (composer/arranger fields) — `idea`

The composer/arranger pickers in the sheet create/edit form (`sheet-form.html`) use a
PrimeNG `p-select` with `[filter]="true"` — it loads the *entire* musician list up front
and filters client-side, not a real typeahead against the backend. Fine at today's scale;
will get slow to load and unwieldy to scroll once the musician table grows.

Replace with `p-autocomplete` backed by a debounced `GET /musicians?search=...` call
(the search infrastructure — trigram/phonetic matching — already exists for sheets and
could be reused/extended for musicians).

**Stakeholders:** S1 (music librarian)
**Effort:** Low (existing search patterns to reuse; swap one form control)

---

### Sticky actions column on scrollable tables — `done`

Applied `[scrollable]="true"` + `pFrozenColumn alignFrozen="right"` on the Actions
column across all `p-table`s with row actions (sheets, musicians, instruments,
ensembles, collections, collection items, sheet↔collection membership, ensemble
voices/members/voice-options, shares, uploads, sheet-detail's instrumentation
tables) — the actions column now stays pinned to the right edge while a table
scrolls horizontally on narrow viewports.

Went further than originally scoped: below 640px, multi-button actions collapse
into a single kebab (`pi-ellipsis-v`) button opening a `p-menu` popup, instead of
staying as 2–4 separate icon buttons squeezed into a frozen column. Built as a
reusable `<app-row-actions [items]="rowMenuItems(row)">` component
(`shared/components/row-actions/`) wrapping projected wide-mode buttons + the
narrow-mode kebab/menu, backed by global `.actions-wide`/`.actions-narrow` CSS
classes (`styles.scss`) — originally a one-off pattern in the uploads feature,
now promoted to a shared component and reused everywhere, including inside
nested dialogs (verified 3 levels deep: ensemble → voice edit dialog → Options
tab table). `BaseCrudList` (musicians/instruments/ensembles/collections' shared
base class) gained a default `rowMenuItems()` (edit + delete) so those four
features needed zero boilerplate beyond wrapping their existing buttons.

**Stakeholders:** S1 (music librarian), S2 (Dirigent)
**Effort:** Low (ended up Medium once the mobile kebab pattern was generalized)

---

### Advanced combined search — `idea`

A filter builder that combines multiple dimensions in a single query. Today genre, first
letter, tag and favourite combine (AND), but only without a search term: as soon as the
full-text query `q` is set, the other filters are ignored. Coverage status is shown per
ensemble but can't be filtered on.

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

### Align UI with the improved Claude Design — `idea`

A visual/interaction redesign of SAM already exists as Claude Design mockups
(`Hi-Fi Shell (PrimeNG).html`, `Sheets Overview (PrimeNG).html`, `Sheet Detail (PrimeNG).html`
/ `v2`, `Create Flows (PrimeNG).html`, `Coverage Breakdown (PrimeNG).html`,
`Classify (PrimeNG).html`). So far these mockups have only been mined for **data model
and feature gaps** (see Open Question #10 and the resulting [implementation plans](plans/README.md)) —
the improved **look & feel itself** (shell layout, page compositions, visual hierarchy,
component styling) has not been implemented.

Revisit the mockups and align the implemented UI with them where the design is an
improvement over the current Sakai-based layout. Likely incremental: start with the
highest-traffic pages (shell/navigation, sheets overview, sheet detail) rather than a
big-bang restyle.

**Stakeholders:** All (UI-wide)
**Effort:** Medium–High (scope depends on how much of the design is adopted)
**Depends on:** The feature-gap plans derived from the same mockups (many are `planned`
/ `in progress`) — implementing those first avoids restyling screens that are about to
change structurally.

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

### Search by mood or description — `idea`

Semantic search with text embeddings (PostgreSQL `pgvector`, embeddings via the
configured LangChain4j provider): find sheets with descriptions like "festive opener,
about 3 minutes, not too hard" or "something calm for a church service", and show
"similar pieces" on the sheet detail page and in the Explore view.

Each sheet gets an embedding of its title, genre, style, notes, tags and (optionally)
AI-written description, refreshed when the sheet changes. It complements, rather than
replaces, the existing full-text, trigram and phonetic search. The AI setlist assistant
could use it as an extra retrieval tool.

**Stakeholders:** S2 (Dirigent), S1 (music librarian)
**Effort:** Medium (pgvector extension, embedding job, search endpoint, UI)

---

## 8. Open Questions

These are unresolved decisions that will affect multiple features. They should be
answered before the relevant implementation work begins.

| # | Question | Affects | Status |
|---|----------|---------|--------|
| 1 | Should a `Musician` user account link to the existing `Musician` entity, or be a separate `User` entity? | Auth, musician–instrument assignment, "my parts" view | **Resolved:** `userId` (OIDC subject) added to `Musician` — no separate User entity. External/historical musicians have `userId = null`. |
| 2 | How is "selected content" for guests scoped — per-sheet flag, collection-based sharing, or ensemble-based? | Guest access, setlist public page | **Resolved:** Resource-scoped share tokens implemented (one token = one sheet instrumentation or collection). Public setlist/sheet pages live at `/share/{token}`. Open-URL anonymous access (no link) intentionally deferred. |
| 3 | Should document-level visibility be independently configurable, or always inherited from the sheet/instrumentation? | Shared document links, guest access | Open |
| 4 | Is anonymous guest access (no link, open public URL) ever desirable? | Guest access scope | Open |
| 5 | Should coverage snapshots be invalidated automatically, or remain manual? | Coverage accuracy, performance | Open: **blocks a Now item.** Proposed: automatic per-sheet recompute, async full recompute on voice changes ([plan](plans/coverage-snapshot-invalidation.md)) |
| 6 | Should lending / checkout be tracked per instrumentation or per physical copy? (Relevant if multiple copies per instrumentation are ever supported) | Lending, physical archive | Open |
| 7 | Which OIDC provider? Self-hosted (Keycloak) or SaaS (Auth0, Google)? | Auth implementation | **Resolved:** Self-hosted Keycloak 26. |
| 8 | Should IP addresses be stored in the document access log, or omitted/anonymised? Requires GDPR/privacy policy decision. | Document access log | **Resolved: not stored.** `userId` (OIDC sub) + snapshotted `username` give unambiguous attribution; IP adds GDPR obligations without meaningful benefit in an ensemble context. |
| 9 | What retention period for the document access log? (e.g. 12 months) | Document access log | Open: **blocks a Now item.** Proposed: 12 months, configurable ([plan](plans/event-log-retention.md)) |
| 10 | All Claude Design files have now been reviewed for data model gaps. Resulting plans are in [implementation plans](plans/README.md) (open ones) and the roadmap. | Multiple | **Resolved** |
