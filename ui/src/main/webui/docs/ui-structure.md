# SAM — UI Structure & Feature Guide

This document describes the user-facing structure, navigation flow, and interaction patterns of
the SAM (Sheet music Archiving & Management) Angular frontend. It is intended as a product-level
reference — for the technical/developer architecture see [`ARCHITECTURE.md`](../ARCHITECTURE.md).

---

## Table of Contents

1. [App Shell & Navigation](#1-app-shell--navigation)
2. [Sheets](#2-sheets)
3. [Collections](#3-collections)
4. [Musicians](#4-musicians)
5. [Ensembles](#5-ensembles)
6. [Instruments](#6-instruments)
7. [Uploads](#7-uploads)
8. [User Preferences](#8-user-preferences)
9. [Home](#9-home)
10. [Common UI Patterns](#10-common-ui-patterns)
11. [Data Model Relationships](#11-data-model-relationships)

---

## 1. App Shell & Navigation

### Layout

The app uses a standard sidebar-plus-content shell:

```
┌─────────────────────────────────────────────────────┐
│  Topbar: [≡] SAM               [EN|DE]  [☀/☾]      │
├──────────┬──────────────────────────────────────────┤
│          │                                          │
│ Sidebar  │   <router-outlet>                        │
│          │                                          │
│ (nav     │   Feature page renders here              │
│  items)  │                                          │
│          │                                          │
└──────────┴──────────────────────────────────────────┘
```

- **Topbar** — collapse/expand toggle (desktop), mobile drawer opener, app name, language
  toggle (EN ↔ DE), dark/light mode toggle.
- **Sidebar** — always visible on desktop; collapses to icon-only strip. On mobile it slides in
  as a drawer.
- **Router outlet** — all features are lazy-loaded; the sidebar and topbar remain mounted at all
  times.

### Navigation Structure

| Section | Item          | Route                    | Icon            |
|---------|---------------|--------------------------|-----------------|
| —       | Home          | `/`                      | —               |
| Main    | Sheets        | `/sheets`                | `pi-file`       |
|         | Collections   | `/collections`           | `pi-folder`     |
|         | Uploads       | `/uploads`               | `pi-upload`     |
|         | Musicians     | `/musicians`             | `pi-user`       |
| Admin   | Ensembles     | `/admin/ensembles`       | `pi-users`      |
|         | Instruments   | `/admin/instruments`     | `pi-sliders-h`  |
|         | Configuration | `/admin/configuration`   | `pi-wrench`     |
| User    | Preferences   | `/user/preferences`      | —               |

The **Admin** group covers reference/configuration data that changes infrequently (instrument
catalogue, ensemble definitions). The **Main** group covers the day-to-day working area.

---

## 2. Sheets

Sheets are the central entity. A sheet represents a single musical piece and carries all its
metadata, attached documents (score PDFs, parts, etc.), and instrumentation assignments.

### 2.1 List Page (`/sheets`)

#### View Modes

The list can be displayed as **Cards** (default) or **Table**. The preference is persisted per
user and can be changed via the toggle in the toolbar. It can also be set globally in
[User Preferences](#8-user-preferences).

#### Card View

Each card shows:
- Title and subtitle
- People row — composer, arranger (with role icons); publisher
- Inline tags — genre, style, year of composition
- User-defined `#hashtag` labels
- Hover-only action overlay in the top-right corner (edit, delete, favorite toggle)

#### Table View

| Column     | Notes                              |
|------------|------------------------------------|
| ★          | Favorite toggle                    |
| Title      | Title + subtitle stacked           |
| Composer   | —                                  |
| Genre      | Translated label                   |
| Style      | Translated label                   |
| Year       | Year of composition                |
| Actions    | Edit (→ form page), delete         |

#### Filters & Search

- **Free-text search** — debounced 300 ms; matches title, subtitle, composer name
- **Genre dropdown** — only lists genres that exist in the current result set (adaptive)
- **A–Z alphabet bar** — filters by first letter of the title; greyed-out letters have no
  matches for the active genre filter
- **Active-filter badge** — shows the number of active filters on the filter panel toggle
- **"Clear filters" link** — resets all filters at once

Pagination: 20 / 50 / 100 rows.

#### Actions

| Action        | Behaviour                                                      |
|---------------|----------------------------------------------------------------|
| New sheet     | Navigates to the full create form (`/sheets/new`)              |
| Click card/row| Opens the detail drawer on ≥ 960 px; navigates to detail page on mobile |
| Edit          | Navigates to the edit form (`/sheets/:id/edit`)                |
| Delete        | Confirmation dialog with the sheet title; deletes on confirm   |

---

### 2.2 Detail — Sidebar Drawer (compact mode)

Appears as a 36 rem panel sliding in from the right while the sheet list stays visible.

**Header row** (single line):

```
[★ fav]  Title — Subtitle                 [↗ new tab]  [✎ edit]
```

- Favorite star toggles immediately
- External-link icon opens the full-page view in a new browser tab
- Pencil icon navigates to the edit form (tooltip: "Edit sheet")

**Tabbed content:**

#### Tab 1 — Details

| Panel            | Content                                                                        |
|------------------|--------------------------------------------------------------------------------|
| Base Data        | Composer, arranger, publisher, genre, style, year, difficulty, tags (read-only)|
| Documents        | File list (filename, size, download/delete); upload button in panel header     |
| Notes            | Free-text additional notes (collapsed by default)                              |
| Legal / Misc     | Publisher IPI, edition, copyright, GEMA work number, ISWC, rating (collapsed) |

#### Tab 2 — Instrumentations

- Table of instrument assignments: instrument name, part label, clef, actions (edit/delete)
- "New instrumentation" button → opens an add/edit dialog

---

### 2.3 Detail — Full Page (`/sheets/:id`)

The same data as the drawer but in a wider, expanded layout:

- Base Data and Documents panels are displayed **side by side** (CSS 2-column grid)
- All sections are present as collapsible panels — no tabs, no separate Instrumentations tab
- The Documents panel gains a **drag-drop zone** for file uploads
- Instrumentations appear as a separate collapsible panel below the top row

---

### 2.4 Create / Edit Form (`/sheets/new`, `/sheets/:id/edit`)

Full-page form with no max-width constraint.

**Base Data fields:**

| Field               | Type                                   | Required |
|---------------------|----------------------------------------|----------|
| Title               | Text input                             | Yes      |
| Subtitle            | Text input                             | No       |
| Composer            | Autocomplete (against musicians list)  | No       |
| Arranger            | Autocomplete (against musicians list)  | No       |
| Genre               | Select (translated enum)               | No       |
| Style               | Select (translated enum)               | No       |
| Year of composition | Number input                           | No       |
| Publisher           | Text input                             | No       |
| Difficulty level    | Free text (e.g. "Grade 4", "3/5")      | No       |
| Edition             | Text input                             | No       |
| Copyright           | Text input                             | No       |
| Additional notes    | Textarea                               | No       |
| Tags                | Chip input (comma or Enter separated)  | No       |

**Documents panel** — only shown in edit mode (a sheet must be saved before documents can be
attached). Supports file input and drag-drop; same document list as the detail views.

---

### 2.5 Sub-Feature: Instrumentations

An instrumentation record links an instrument to a sheet part. Managed via a dialog launched from
both the sidebar detail drawer (Instrumentations tab) and the full-page detail.

**Fields:** Instrument (required, select), Part label, Clef, Notes, Notation type

---

## 3. Collections

A collection groups sheets together with an ordering identifier. Two types are supported:

| Type     | Meaning                                       |
|----------|-----------------------------------------------|
| FOLDER   | Generic grouping (e.g. "Marches", "Season 2024") |
| SETLIST  | Ordered performance programme                 |

### 3.1 List Page (`/collections`)

**View modes:** Cards (default) or Table.

#### Table View

| Column      | Notes                                  |
|-------------|----------------------------------------|
| Name        | With description below                 |
| Type        | Colour-coded badge (blue = FOLDER, green = SETLIST) |
| Date        | Optional event/programme date          |
| Sheet count | Number of sheets in the collection     |
| Actions     | Edit, delete                           |

#### Card View

Cards show name (clickable link), description, type badge, date, and sheet count.

**Filters:** Name search, type dropdown.

Pagination: 10 / 25 / 50.

**Create and edit** both use a compact dialog (32 rem) — there is no separate form page.

---

### 3.2 Detail Page (`/collections/:id`)

Not a form page — read-only header followed by the sheet membership list.

**Header block:**
- Collection name + type badge
- Description (if set)
- Date with calendar icon (if set)
- Quick-actions bar: "Generate TOC" button

**CollectionSheets table:**

| Column     | Notes                                                      |
|------------|------------------------------------------------------------|
| Identifier | Programme number / sort key (e.g. "1A", "Opener")         |
| Title      | Sheet title, links to the sheet list                       |
| Genre      | Translated label                                           |
| Actions    | Preview, edit identifier, remove                           |

#### Add Sheet Dialog (44 rem)

1. Search bar — live search against the full sheet archive
2. Selectable results table
3. Identifier field (required) — unique label for this sheet within the collection
4. Save

#### Edit Dialog (26 rem)

- Sheet name shown as a locked read-only info block
- Only the identifier field is editable

#### Preview

Read-only metadata card showing the linked sheet's full details.

---

## 4. Musicians

The musician catalogue is primarily reference data used as autocomplete values in the
sheet form (Composer, Arranger fields).

### List Page (`/musicians`)

Table-only (no card view).

| Column     | Notes                                          |
|------------|------------------------------------------------|
| Name       | Required                                       |
| IPI        | International Standard Name Identifier (ISNI variant) |
| Birth year | Optional                                       |
| Death year | Optional                                       |
| Actions    | Edit, delete                                   |

**Search:** Name-based, debounced.

**Create / Edit:** Dialog (28 rem). No separate detail page — musicians are supporting data.

---

## 5. Ensembles

An ensemble models a band or orchestra as a set of **voices** (player seats). Each voice defines
which instruments can fill that seat via **voice options**, and carries metadata (weight, required
flag) used for coverage scoring.

### 5.1 List Page (`/admin/ensembles`)

**View modes:** Cards (default) or Table.

**Table columns:** Name, description, voice count, actions.

**Cards** show: name, description (2-line clamp), voice count as a tag.

**Create / Edit:** Dialog (28 rem). Clicking a row or card navigates to the detail page.

---

### 5.2 Detail Page (`/admin/ensembles/:id`)

Header shows ensemble name and description. The body is entirely the **EnsembleVoices**
sub-component.

#### Voices Table

| Column      | Notes                                              |
|-------------|----------------------------------------------------|
| Label       | Voice name (e.g. "1st Clarinet")                   |
| Required    | Flag — must this seat be covered?                  |
| Weight      | 0.0–1.0 scoring weight for coverage calculations  |
| Options     | Number of instrument options defined               |
| Actions     | Open-on-Options tab, open-on-Details tab, delete   |

The pencil button opens the edit dialog on the **Details tab**; the gear button opens it on the
**Options tab**.

#### Add Voice Dialog (26 rem)

Fields: label (required), weight (number 0–1), required checkbox.

After saving, the dialog seamlessly **transitions to the tabbed edit dialog** already open on the
Options tab — so the user can immediately start adding instrument options.

#### Edit Voice Dialog (44 rem) — Tabbed

**Tab 1 — Details:**
- Voice name shown as a locked read-only info block
- Editable: weight, required checkbox

**Tab 2 — Options:**
- Table of instrument options

---

### 5.3 Sub-Feature: Voice Options

A voice option specifies that a particular instrument can fill a voice, with an optional type and
scoring factor.

| Column        | Notes                                              |
|---------------|----------------------------------------------------|
| Instrument    | Name                                               |
| Transposition | e.g. Bb, Eb                                        |
| Type          | PRIMARY / ALTERNATE / FALLBACK                     |
| Factor        | 0.0–1.0 scoring multiplier                         |
| Actions       | Edit, delete                                       |

#### Add Option Dialog (44 rem)

1. Search bar — client-side filter against the full instrument list
2. Selectable instrument table (instrument name + transposition)
3. Selected-instrument indicator
4. Type select + Factor number input (in a footer row)
5. Save (enabled only once an instrument is selected)

#### Edit Option Dialog (26 rem)

- Instrument name shown as a locked read-only info block
- Only Type and Factor are editable

---

## 6. Instruments

The instrument catalogue. Instruments are referenced by instrumentations (on sheets) and by voice
options (on ensemble voices).

### List Page (`/admin/instruments`)

Table-only.

| Column       | Notes                                      |
|--------------|--------------------------------------------|
| ID           | Auto-assigned                              |
| Name         | Required (e.g. "Clarinet")                 |
| Display name | Optional long-form label                   |
| Transposition| C / D / Eb / F / G / A / Ab / Bb           |
| Actions      | Edit, delete                               |

**Create / Edit:** Dialog (28 rem). No separate detail page.

---

## 7. Uploads

A staging area for bulk file ingestion. Files uploaded here create untyped attachment records
that can later be associated with specific sheets.

### Page (`/uploads`)

**Three sections:**

1. **Drop zone / file picker** — PrimeNG advanced file upload; supports multi-file selection and
   drag-drop.
2. **Active uploads** — live progress list while files are transferring (filename, progress bar,
   percentage).
3. **Uploaded documents table** — filename, size, MIME type, SHA-256 checksum (truncated);
   per-row download and delete actions. Paginated at 20 per page.

---

## 8. User Preferences

### Page (`/user/preferences`)

Three independent settings groups:

| Group              | Options                                      | Persistence          |
|--------------------|----------------------------------------------|----------------------|
| Appearance         | Light / Dark theme                           | `localStorage`       |
| Language           | EN / DE                                      | `localStorage`       |
| Layout preferences | Cards / List per feature (Sheets, Collections, Ensembles) | `localStorage` |

Layout preferences can also be toggled directly from each list page via the view-mode button in
the toolbar.

---

## 9. Home

### Page (`/`)

A landing/dashboard page with two groups of navigation cards:

**Main:**
- Sheets, Collections, Uploads, Musicians

**Admin:**
- Ensembles, Instruments, Configuration

Each card shows an icon, title, and a short description of the section. Clicking navigates to
the corresponding route.

---

## 10. Common UI Patterns

### 10.1 CRUD Complexity Tiers

| Tier | Pattern | Used by |
|------|---------|---------|
| Simple | Dialog create/edit, table-only list | Musicians, Instruments |
| Moderate | Dual-view list (cards/table), dialog create/edit, separate detail page | Collections, Ensembles |
| Complex | Dual-view list, full-page form, detail drawer + full-page detail | Sheets |

### 10.2 Two-Dialog Add/Edit Pattern

Used wherever a user picks an **existing entity** and attaches **metadata** to the link:

- **Add dialog (~44 rem)** — searchable/filterable entity table + metadata footer; Save enabled
  only once an entity is selected
- **Edit dialog (~26 rem)** — linked entity shown as a locked read-only info block; only the
  metadata fields are editable

This pattern is used in:
- CollectionSheets (sheet → identifier)
- Voice Options (instrument → type + factor)

### 10.3 Tabbed Dialogs

Used when a child entity has two logically separate editing surfaces that would otherwise require
stacking dialogs:

- **EnsembleVoices edit dialog** — Tab 1: voice properties (weight, required); Tab 2: voice
  options table with full add/edit/delete
- **SheetDetail sidebar** — Tab 1: sheet details (metadata, documents, notes, legal); Tab 2:
  instrumentations table

### 10.4 Sidebar Drawer Detail

On the Sheets list (≥ 960 px), clicking a sheet opens a 36 rem detail drawer from the right
while keeping the list visible in the background. This allows fast browsing without full
navigations. On smaller viewports the full-page detail is used instead.

### 10.5 Seamless Post-Create Transition

When a new child entity is created and it has sub-entities to configure immediately, the UI
transitions directly from the "Add" dialog to the "Edit" dialog (already open on the relevant
tab) rather than closing and requiring a second click. Example: after adding a new voice, the
dialog switches to the Edit Voice dialog on the Options tab.

### 10.6 View Mode Toggle & Persistence

Sheets, Collections, and Ensembles list pages support Cards / Table toggle. The chosen mode is
stored in `localStorage` via `LayoutPreferenceService` and restored on next visit.

### 10.7 Adaptive Genre Filter

On the Sheets list, the Genre dropdown only shows genres that exist in the current filtered
result set. This prevents the user from selecting a combination that would return zero results.

---

## 11. Data Model Relationships

```
Musician ◄──────────── SheetMusic ────────────► Attachment (Document)
               composer/arranger         │
                                         │
                               Instrumentation
                                         │
                                    Instrument
                                         │
                          ┌──────────────┘
                          │
Ensemble ──── EnsembleVoice ──── VoiceOption ──► Instrument
                    │
                 (weight,
                 required,
                 options)

SheetCollection ──── CollectionSheet ──► SheetMusic
     (FOLDER /
      SETLIST)
```

### Key Relationships

| Relationship | Cardinality | Notes |
|---|---|---|
| SheetMusic → Musician (composer) | many-to-one | Optional; resolved via autocomplete |
| SheetMusic → Musician (arranger) | many-to-one | Optional; resolved via autocomplete |
| SheetMusic → Instrumentation | one-to-many | Which instruments play which parts |
| Instrumentation → Instrument | many-to-one | Required |
| SheetMusic → Attachment | one-to-many | PDF scores, parts, etc. |
| SheetCollection → CollectionSheet → SheetMusic | many-to-many (with metadata) | Identifier is the join-table attribute |
| Ensemble → EnsembleVoice | one-to-many | Player seats |
| EnsembleVoice → VoiceOption → Instrument | many-to-many (with metadata) | Type + factor on the join |

### Enumerations

| Enum | Values |
|---|---|
| Genre | MARCH, MARCHING_SHOW, CONCERT_WORK, FANFARE, OVERTURE, SUITE, HYMN, CHORALE, WALTZ, POLKA, TANGO, FOXTROT, BALLAD, MEDLEY, POP_ARRANGEMENT, JAZZ_ARRANGEMENT, FILM_MUSIC, SPIRITUAL, GOSPEL, FOLK, OTHER |
| Style | CLASSICAL, ROMANTIC, MODERN, CONTEMPORARY, JAZZ, SWING, BIG_BAND, POP, ROCK, FOLK, GOSPEL, FILM |
| CollectionType | FOLDER, SETLIST |
| VoiceOptionType | PRIMARY, ALTERNATE, FALLBACK |
| Clef | TREBLE, ALTO, TENOR, BASS |
| NotationType | STANDARD, TABLATURE, PERCUSSION, LEAD_SHEET, GRAPHIC |
| Transposition | C, D, Eb, F, G, A, Ab, Bb |
