# SAM — UI Design Guide

This document defines the visual language, layout conventions, and interaction patterns that
keep the SAM UI consistent across all features. When building a new page, component, or dialog,
use this as the first reference for how things should look and behave.

For what each feature does, see [`ui-structure.md`](./ui-structure.md).
For code architecture and file conventions, see [`ARCHITECTURE.md`](../ARCHITECTURE.md).

---

## Table of Contents

1. [Design Tokens](#1-design-tokens)
2. [Typography](#2-typography)
3. [Spacing Scale](#3-spacing-scale)
4. [Page Layout](#4-page-layout)
5. [Toolbar Pattern](#5-toolbar-pattern)
6. [Filter Panel](#6-filter-panel)
7. [Tables](#7-tables)
8. [Cards](#8-cards)
9. [Dialogs](#9-dialogs)
10. [Forms](#10-forms)
11. [Buttons](#11-buttons)
12. [Detail Pages & Drawers](#12-detail-pages--drawers)
13. [Sub-Component Panels](#13-sub-component-panels)
14. [Read-Only Info Blocks](#14-read-only-info-blocks)
15. [State & Feedback](#15-state--feedback)
16. [Shared CSS Classes Reference](#16-shared-css-classes-reference)

---

## 1. Design Tokens

All colors, surfaces, and borders are expressed as CSS custom properties defined in
`src/styles.scss`. **Never use raw hex values or PrimeNG surface tokens in feature SCSS.**
Always use a `--sam-*` variable so components adapt correctly in both light and dark mode.

### Color Palette

| Token | Light | Dark | Usage |
|---|---|---|---|
| `--sam-bg` | `#f4f3f0` | `#111114` | Page background (behind panels/cards) |
| `--sam-surface` | `#ffffff` | `#1a1a1f` | Card, panel, dialog backgrounds |
| `--sam-border` | `#e4e2dd` | `#2a2a32` | All borders |
| `--sam-hover` | `rgba(0,0,0,.04)` | `rgba(255,255,255,.06)` | Row hover, subtle hover states |
| `--sam-active-bg` | `rgba(45,50,80,.08)` | `rgba(140,150,210,.12)` | Selected / active states |
| `--sam-accent` | `#3b4070` | `#9ba2d0` | Brand colour (logo, links) |
| `--sam-text` | `#1a1a1e` | `#e8e8ec` | Primary text |
| `--sam-text-secondary` | `#4a4a52` | `#a0a0ac` | Labels, secondary copy |
| `--sam-text-muted` | `#8a8a96` | `#606068` | Subtitles, placeholders, empty states |

PrimeNG's own `--p-*` tokens are used only for PrimeNG component internals (e.g.
`var(--p-border-radius)`, `var(--p-primary-color)`). Do not use `--p-surface-*` in custom
layouts; use `--sam-surface` and `--sam-border` instead.

---

## 2. Typography

Font family: **DM Sans** for both headings and body text (`--sam-font-body`,
`--sam-font-display`).

### Heading hierarchy

| Context | Element | Size | Weight | Color | Class |
|---|---|---|---|---|---|
| Page list title (toolbar) | `h1` inside `.page-toolbar` | 1.5 rem | 600 | `--sam-text` | — (styled by `.page-toolbar h1`) |
| Detail page title (full-width) | `h1` inside `.detail-header` | 1.75 rem | 600 | `--sam-text` | — (styled by `.detail-header h1`) |
| Detail drawer title | `h2` inside `.detail-header` (drawer) | 1.25 rem | 600 | `--sam-text` | — (styled by `_details.scss`) |
| Sub-component section heading | `h3` | 1.1 rem | 600 | `--sam-text` | inline or in toolbar |

### Body text roles

| Token / class | Size | Color | Used for |
|---|---|---|---|
| default `<p>` | 1 rem | `--sam-text` | Body copy |
| `.page-subtitle` | 0.85 rem | `--sam-text-muted` | Page toolbar subtitle |
| `.detail-description` | 0.95 rem | `--sam-text-secondary` | Detail page description |
| `.detail-subtitle` | 0.9 rem | `--sam-text-muted` | Drawer subtitle (below h2) |
| `.detail-label` | 0.75 rem, 600, uppercase, 0.025 em tracking | `--sam-text-muted` | Metadata field labels |
| `.detail-value` | 0.9 rem | `--sam-text` | Metadata field values |
| `.empty-block` | 0.9 rem, italic | `--sam-text-muted` | "No items" text inside panels |

---

## 3. Spacing Scale

Consistent spacing is achieved by following these values rather than arbitrary pixel/rem amounts.

| Purpose | Value |
|---|---|
| Gap between major page sections (toolbar → filter → table) | `1rem` |
| Gap between detail panels | `0.75rem` |
| Gap between toolbar actions (buttons, search) | `0.75rem` |
| Gap inside `.form-row` (side-by-side fields) | `1rem` |
| Gap between form fields in `.sam-form` | `1.25rem` |
| Gap between label and input in `.form-field` | `0.375rem` |
| Gap between card grid items | `1rem` |
| Gap between row-action icon buttons | `0.25rem` |
| Gap between icon buttons in compact overlays | `0.125rem` |
| Dialog form cancel + save button gap | `0.5rem` |

---

## 4. Page Layout

Every feature list page follows the same outer wrapper pattern:

```html
<div class="[feature]-page">       <!-- flex column, gap: 1rem -->
  <div class="page-toolbar"> … </div>
  <!-- optional: filter panel -->
  <!-- optional: alpha bar (sheets only) -->
  <p-table …> … </p-table>         <!-- or card grid -->
</div>
```

The `[feature]-page` wrapper sets `display: flex; flex-direction: column; gap: 1rem`. Only the
feature-specific class name differs between pages.

Detail pages that are full-width routes (no max-width) use:

```html
<div class="[feature]-detail-page">   <!-- flex column, gap: 1.5rem -->
  <div class="page-toolbar"> … </div>  <!-- back button + edit action -->
  <div class="detail-header"> … </div> <!-- entity title block -->
  <!-- sub-component(s) -->
</div>
```

---

## 5. Toolbar Pattern

The page toolbar is the consistent top strip of every list and detail page.

### Anatomy

```
┌─────────────────────────────────────────────────────────┐
│  [Title group]                      [Toolbar actions]   │
│   h1 Page Title                      [search] [filter▾] │
│   p  subtitle                        [≡|⊞] [+ New]     │
└─────────────────────────────────────────────────────────┘
```

```html
<div class="page-toolbar">
  <div class="page-title-group">
    <h1>{{ 'feature.title' | translate }}</h1>
    <p class="page-subtitle">{{ 'feature.subtitle' | translate }}</p>
  </div>
  <div class="toolbar-actions">
    <!-- search field (if used in toolbar) -->
    <!-- filter toggle (if feature has a filter panel) -->
    <!-- view mode toggle (if feature supports cards/list) -->
    <!-- primary create button -->
  </div>
</div>
```

### Toolbar action ordering (left → right)

1. **Search field** — always leftmost action
2. **Filter toggle button** — only on pages with a separate filter panel (Sheets, Collections)
3. **View mode toggle** (`p-selectbutton`) — on pages that support card/list switching
4. **Primary create button** — always rightmost, always with `icon="pi pi-plus"` and a label

### Search field

```html
<p-iconfield>
  <p-inputicon styleClass="pi pi-search" />
  <input pInputText type="text" [placeholder]="t.t('feature.search')" (input)="onFilter($event)" />
</p-iconfield>
```

Search is **debounced 300 ms** in the component. Never trigger a server call on every keystroke.
The search field is **always placed in the toolbar** — never inside the table `#caption` template.

### View mode toggle

```html
<p-selectbutton [options]="viewOptions" [(ngModel)]="viewMode" optionLabel="icon" optionValue="value">
  <ng-template #item let-item><i [class]="item.icon"></i></ng-template>
</p-selectbutton>
```

Options are `{ value: 'cards', icon: 'pi pi-th-large' }` and `{ value: 'list', icon: 'pi pi-list' }`.

### Filter toggle button

When a filter panel exists, the toggle button shows an active count badge:

```html
<p-button [outlined]="true" [severity]="activeFilterCount() > 0 ? 'primary' : 'secondary'"
          (onClick)="toggleFilterPanel()">
  <i class="pi pi-filter"></i>
  <span>{{ 'feature.filters.toggle' | translate }}</span>
  @if (activeFilterCount() > 0) {
    <p-badge [value]="'' + activeFilterCount()" />
  }
</p-button>
```

---

## 6. Filter Panel

Used when a feature has criteria beyond a single search field. Appears below the toolbar,
always visible (no collapse animation needed — the toolbar filter button toggles it with `@if`).

```html
<div class="filter-panel">
  <div class="filter-row">
    <div class="filter-field">
      <label>{{ 'feature.filters.someField' | translate }}</label>
      <p-select … />
    </div>
    <div class="filter-field">
      <label>{{ 'feature.filters.anotherField' | translate }}</label>
      <p-select … />
    </div>
    @if (hasActiveFilters()) {
      <span class="clear-filters" (click)="clearFilters()">
        {{ 'feature.filters.clear' | translate }}
      </span>
    }
  </div>
</div>
```

- Filter fields use a `label` + `p-select` (or similar) without `p-floatlabel` — labels are
  always visible in filter panels.
- The "Clear filters" link appears only when at least one filter is active.
- Filter selects always include `[showClear]="true"` so a single field can also be reset
  individually.

---

## 7. Tables

All data tables use PrimeNG `p-table` with server-side lazy loading. The structure is identical
across all features — only the columns change.

### Standard table template

```html
<p-table [value]="items()" [lazy]="true" [paginator]="true" [rows]="rows"
         [totalRecords]="totalRecords()" [loading]="loading()"
         [rowsPerPageOptions]="[10, 25, 50]" (onLazyLoad)="onLazyLoad($event)">

  <ng-template #header>
    <tr>
      <th>{{ 'feature.columns.name' | translate }}</th>
      <!-- … more columns … -->
      <th>{{ 'feature.columns.actions' | translate }}</th>  <!-- always last -->
    </tr>
  </ng-template>

  <ng-template #body let-item>
    <tr [class.clickable-row]="rowIsClickable">
      <td>{{ item.name }}</td>
      <!-- … more cells … -->
      <td class="actions-cell">
        <!-- row action buttons -->
      </td>
    </tr>
  </ng-template>

  <ng-template #emptymessage>
    <tr>
      <td [attr.colspan]="columnCount" class="empty-message">
        {{ 'feature.empty' | translate }}
      </td>
    </tr>
  </ng-template>
</p-table>
```

### Actions cell

Row actions always go in the **last column**, using `.actions-cell` (flex row, 0.25 rem gap).
Buttons are always icon-only, small, rounded, text style:

```html
<td class="actions-cell">
  <!-- optional: context-specific action with tooltip -->
  <p-button icon="pi pi-cog" [rounded]="true" [text]="true" size="small"
            [pTooltip]="t.t('feature.someAction')" (onClick)="doSomething(item)" />

  <!-- edit — always second-to-last -->
  <p-button icon="pi pi-pencil" [rounded]="true" [text]="true" size="small"
            (onClick)="openEdit(item)" />

  <!-- delete — always last, always danger -->
  <p-button icon="pi pi-trash" [rounded]="true" [text]="true" size="small"
            severity="danger" (onClick)="confirmDelete(item)" />
</td>
```

**Icon conventions for row actions:**

| Icon | Meaning |
|---|---|
| `pi-pencil` | Edit / open edit dialog or form |
| `pi-trash` | Delete (always `severity="danger"`) |
| `pi-cog` | Configure / open options (always with tooltip) |
| `pi-eye` | Preview / read-only detail view |
| `pi-external-link` | Open in new tab |

Use tooltips (`pTooltip`) on any icon whose meaning is ambiguous, especially when three or more
icons appear on the same row.

### Clickable rows

When a row click navigates or opens a detail view, add `.clickable-row` to the `<tr>`:

```html
<tr class="clickable-row" (click)="openDetail(item)">
```

### Pagination row counts

| Context | Options |
|---|---|
| Main feature list (Sheets) | 20, 50, 100 |
| Standard reference list | 10, 25, 50 |
| Sub-component table | none (loads all via `FETCH_ALL_SIZE`) or 10, 25, 50 |

---

## 8. Cards

Used as an alternative to the table view in Sheets, Collections, and Ensembles. Cards always
live in a CSS auto-fill grid:

```scss
.feature-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));  // or 300px for wider cards
  gap: 1rem;
}
```

### Card actions — hover overlay

Actions float in the top-right corner, hidden until the card is hovered. This is the **only**
card-actions pattern used across all card views (Collections, Ensembles).

```scss
.feature-card {
  position: relative;
  // …
  &:hover .card-actions { opacity: 1; }
}

.card-actions {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  display: flex;
  gap: 0.125rem;
  opacity: 0;
  transition: opacity 0.15s;
  background: var(--sam-surface);
  border: 1px solid var(--sam-border);
  border-radius: var(--p-border-radius);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  padding: 0.125rem;
}
```

### Card structure

```html
<div class="feature-card">
  <div class="card-header">
    <span class="card-title">Name</span>
    <span class="card-subtitle">Secondary info</span>  <!-- or card-description -->
  </div>
  <!-- card body content -->
  <div class="card-actions"> … </div>
</div>
```

---

## 9. Dialogs

### Size conventions

| Width | Use case |
|---|---|
| `26 rem` | Narrow: simple forms (add voice, edit voice details) |
| `28 rem` | Standard: most create/edit dialogs (musicians, instruments, ensembles) |
| `32 rem` | Medium: forms with more fields (collections, sheet instrumentations) |
| `44 rem` | Wide: entity-picker dialogs with a searchable table + metadata footer |

Add `min-height` when the dialog content could otherwise feel too short on initial open
(e.g. a dialog that loads data asynchronously or has a prominent empty state):

```html
<p-dialog [style]="{ width: '26rem', 'min-height': '28rem' }" …>
```

### Standard dialog structure

```html
<p-dialog
  [header]="editingItem ? t.t('feature.edit') : t.t('feature.new')"
  [(visible)]="dialogVisible"
  [modal]="true"
  [style]="{ width: '28rem' }"
>
  @if (dialogVisible) {
    <app-feature-form
      [item]="editingItem"
      (saved)="onSaved()"
      (cancelled)="dialogVisible = false"
    />
  }
</p-dialog>
```

Always wrap form content in `@if (dialogVisible)` to destroy and recreate the form component
on each open — this avoids stale state from a previous editing session.

### Form actions inside dialogs

Cancel always on the left, Save on the right, right-aligned as a block:

```html
<div class="form-actions">
  <p-button [label]="t.t('feature.cancel')" severity="secondary" [text]="true"
            type="button" (onClick)="cancelled.emit()" />
  <p-button [label]="t.t('feature.save')" type="submit"
            [disabled]="form.invalid || saving" [loading]="saving" />
</div>
```

### Confirmation dialogs

Use PrimeNG `ConfirmationService` + `<p-confirmdialog />`. The accept button is always danger:

```typescript
this.confirmationService.confirm({
  message: this.t.t('feature.delete.confirm').replace('{name}', item.name),
  header: this.t.t('feature.delete.header'),
  icon: 'pi pi-exclamation-triangle',
  acceptButtonStyleClass: 'p-button-danger',
  accept: () => { /* delete */ },
});
```

---

## 10. Forms

### Wrapper

Every form (whether in a dialog or on a full page) uses `.sam-form` as the outer container:

```html
<form [formGroup]="form" (ngSubmit)="onSave()" class="sam-form">
  …
  <div class="form-actions"> … </div>
</form>
```

`.sam-form` sets `display: flex; flex-direction: column; gap: 1.25rem`.

### Single field

```html
<div class="form-field">
  <p-floatlabel variant="on">
    <input pInputText inputId="field-id" formControlName="fieldName" />
    <label for="field-id">{{ 'feature.form.fieldName' | translate }}</label>
  </p-floatlabel>
</div>
```

Always use `p-floatlabel variant="on"` — the label floats above the input when the field has a
value. Do **not** use static labels above inputs in forms (static labels belong only in filter
panels and metadata grids).

### Side-by-side fields

```html
<div class="form-row">
  <div class="form-field flex-1"> … </div>
  <div class="form-field flex-1"> … </div>
</div>
```

Add `.flex-1` to each `form-field` inside a `form-row` so they share space equally. Use
different flex values for unequal widths.

### PrimeNG component widths

`p-select`, `p-inputnumber`, `p-datepicker`, and `p-autocomplete` inside a `p-floatlabel` are
forced to `width: 100%` by the global `_forms.scss` rules — no extra `styleClass` needed.
For PrimeNG components **outside** a `p-floatlabel`, add `styleClass="w-full"`.

### Checkbox in a form row

Checkboxes are aligned with adjacent floatlabel inputs using `.form-field--checkbox`:

```html
<div class="form-row">
  <div class="form-field flex-1">
    <p-floatlabel variant="on"> … </p-floatlabel>
  </div>
  <div class="form-field form-field--checkbox">
    <p-checkbox formControlName="required" [binary]="true" inputId="required-chk" />
    <label for="required-chk">{{ 'feature.form.required' | translate }}</label>
  </div>
</div>
```

`.form-field--checkbox` sets `flex-direction: row; align-items: center; align-self: center`
so the checkbox naturally centres against the taller floatlabel column.

### Field with tooltip hint

```html
<div class="field-with-hint">
  <p-floatlabel variant="on">
    <p-select inputId="type" formControlName="type" … />
    <label for="type">{{ 'feature.form.type' | translate }}</label>
  </p-floatlabel>
  <i class="pi pi-info-circle field-hint-icon"
     [pTooltip]="t.t('feature.form.typeTooltip')"
     tooltipPosition="top"></i>
</div>
```

`.field-with-hint` wraps the floatlabel and the info icon in a flex row. The icon is
`0.8rem`, `--sam-text-muted`, brightens to `--sam-text-secondary` on hover.

---

## 11. Buttons

### Primary CTA (create, main action)

```html
<p-button [label]="t.t('feature.new')" icon="pi pi-plus" (onClick)="openNew()" />
```

Filled primary style (default). Always labelled, icon on the left, always rightmost in the
toolbar. The "New" action is always `pi-plus`.

### Secondary / outlined (sub-actions)

```html
<p-button [label]="t.t('feature.someAction')" [outlined]="true" severity="secondary"
          size="small" (onClick)="…" />
```

Used for secondary actions inside panels, dialog header actions, upload buttons. Keep `small`
size inside sub-component panels.

### Icon-only row actions

```html
<p-button icon="pi pi-pencil" [rounded]="true" [text]="true" size="small" (onClick)="…" />
```

Transparent background, circular hit-target, small size. Never add a label — use `pTooltip`
if the icon meaning is not immediately clear. Delete is always `severity="danger"`.

### Back navigation

```html
<p-button icon="pi pi-arrow-left" [label]="t.t('feature.backToList')" [text]="true"
          (onClick)="goBack()" />
```

Text style (no border/background), used as the leftmost item in detail-page toolbars.

### Danger confirmation

Destructive actions (delete, remove) always go through `ConfirmationService` — never trigger
a delete directly on click. The confirmation dialog's accept button uses
`acceptButtonStyleClass: 'p-button-danger'`.

---

## 12. Detail Pages & Drawers

### Full-width detail page

Header area uses the shared `.detail-header` + `.detail-title-row` + `.detail-description`
classes (from `_pages.scss`):

```html
<div class="detail-header">
  <div class="detail-title-row">
    <h1>{{ entity.name }}</h1>
    <!-- optional inline badge -->
  </div>
  @if (entity.description) {
    <p class="detail-description">{{ entity.description }}</p>
  }
</div>
```

`detail-header` provides `flex-direction: column; gap: 0.5rem`. The `h1` is styled at
`1.75 rem / 600` automatically.

### Sidebar drawer (sheet detail, compact mode)

The drawer header is structured differently — actions are inline with the title, not separated:

```html
<div class="detail-header">  <!-- from _details.scss: flex row, space-between -->
  <div class="detail-title-group">
    <!-- optional leading icon button (fav, etc.) -->
    <div class="detail-title-text">
      <div class="detail-title-row">   <!-- h2 + inline icon actions on one line -->
        <h2>{{ entity.title }}</h2>
        <!-- icon-only action buttons (external-link, edit) -->
      </div>
      @if (entity.subtitle) {
        <p class="detail-subtitle">{{ entity.subtitle }}</p>
      }
    </div>
  </div>
</div>
```

The `h2` takes `flex: 1` so title text fills available width and icon buttons are pushed to the
right naturally.

### Tabbing content in the drawer

When a drawer has two logically distinct content groups, use `<p-tabs>` with `[value]="0"` as
the default. Do **not** use tabs in the full-page view — use collapsible `p-panel` sections
instead.

---

## 13. Sub-Component Panels

A sub-component panel is an embedded section with its own title, action button, and content
(table or list). Used in detail pages (Voices in Ensemble, Sheets in Collection).

### Toolbar

```html
<div class="section-toolbar">       <!-- display: flex; justify-content: space-between -->
  <h3>{{ 'feature.section.title' | translate }}</h3>
  <p-button [label]="t.t('feature.section.new')" icon="pi pi-plus"
            size="small" (onClick)="openNew()" />
</div>
```

The `h3` is on the left; the small "New" button is on the right. The button uses `size="small"`
to stay proportional — do not use full-size buttons in embedded panels.

### Panels within dialogs / tab panels

When a section lives inside a dialog tab (not its own page), the same toolbar pattern applies
but the heading level may be omitted if the tab label already names the section.

---

## 14. Read-Only Info Blocks

Used in edit dialogs to show the locked entity that is being operated on (e.g. which instrument
an option belongs to, which voice is being edited). This prevents accidental confusion about
what is being modified.

```html
<div class="editing-entity-info">
  <span class="info-label">{{ 'feature.form.entityLabel' | translate }}</span>
  <span class="info-value">{{ entity.name }}</span>
</div>
```

```scss
.editing-entity-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.75rem 1rem;
  background: var(--sam-surface-raised, var(--sam-surface));
  border: 1px solid var(--sam-border);
  border-radius: var(--p-border-radius);
  margin-bottom: 0.25rem;

  .info-label {
    font-size: 0.8rem;
    font-weight: 500;
    color: var(--sam-text-secondary);
    text-transform: uppercase;
    letter-spacing: 0.03em;
  }

  .info-value {
    font-weight: 500;
    color: var(--sam-text);
  }
}
```

Always place the info block at the **top** of the edit form, above the editable fields.

---

## 15. State & Feedback

### Loading — full area

```html
<div class="loading-indicator">
  <i class="pi pi-spinner pi-spin"></i>
</div>
```

Centered, 3 rem padding, 2 rem icon. Replaces the entire content area while data is loading.

### Loading — inside panel

```html
<div class="loading-indicator small">
  <i class="pi pi-spinner pi-spin"></i>
</div>
```

1 rem padding, 1.2 rem icon. Used within a `p-panel` while the panel's specific content loads.

### Loading — table

Use PrimeNG's built-in `[loading]="loading()"` on `p-table`. This shows a skeleton overlay
automatically — do not add a separate spinner inside table templates.

### Empty states

**In a table** (no rows returned):

```html
<ng-template #emptymessage>
  <tr>
    <td [attr.colspan]="columnCount" class="empty-message">
      {{ 'feature.empty' | translate }}
    </td>
  </tr>
</ng-template>
```

`.empty-message` centres the text, muted colour, `2rem` padding.

**In a panel** (section has no items yet):

```html
<span class="empty-block">{{ 'feature.noItems' | translate }}</span>
```

`.empty-block` is italic, muted, 0.9 rem.

### Success / error toasts

All mutations (create, update, delete) show a toast on success. Use `summary` only (no
`detail`) for short, scannable feedback:

```typescript
this.messageService.add({
  severity: 'success',
  summary: this.t.t('feature.messages.created'),
});
```

Errors are handled by showing a toast with `severity: 'error'`. Never silently swallow errors.

### Delete confirmation

```typescript
this.confirmationService.confirm({
  message: this.t.t('feature.delete.confirm').replace('{name}', item.name),
  header: this.t.t('feature.delete.header'),
  icon: 'pi pi-exclamation-triangle',
  acceptButtonStyleClass: 'p-button-danger',
  accept: () => { … },
});
```

Always interpolate the entity name into the confirmation message so users know exactly what
they are about to delete.

---

## 16. Shared CSS Classes Reference

### Imported via `@use 'pages'`

| Class | Purpose |
|---|---|
| `.page-toolbar` | Outer toolbar row: title-group left, actions right |
| `.page-title-group` | Vertical stack of h1 + subtitle |
| `.page-subtitle` | Muted caption below the h1 |
| `.toolbar-actions` | Flex row for search / toggles / buttons |
| `.filter-panel` | Surfaced container for filter controls |
| `.filter-row` | Flex row of filter fields |
| `.filter-field` | Label + control column |
| `.clear-filters` | Inline "Clear" link, primary colour |
| `.detail-header` | Column header for detail pages (h1 style) |
| `.detail-title-row` | Flex row: h1 + inline badges/actions |
| `.detail-description` | Muted paragraph below the title |
| `.actions-cell` | Flex row for row action buttons |
| `.empty-message` | Centred muted empty-state text in tables |
| `.loading-indicator` | Centred full-area spinner |
| `.loading-indicator.small` | Compact inline spinner |
| `.clickable-row` | Pointer cursor on table rows |
| `.type-badge` | Coloured inline badge (`.type-folder`, `.type-setlist`) |

### Imported via `@use 'forms'`

| Class | Purpose |
|---|---|
| `.sam-form` | Form wrapper: flex column, 1.25 rem gap |
| `.form-field` | Label + input column unit |
| `.form-field.flex-1` | Expanding form field inside a row |
| `.form-row` | Side-by-side field row |
| `.form-field--checkbox` | Inline checkbox + label, self-centred in a row |
| `.form-actions` | Right-aligned cancel + save row |
| `.field-with-hint` | Input + info icon row |
| `.field-hint-icon` | `pi-info-circle` with tooltip, muted |
| `.checkbox-label-row` | Standalone checkbox + label (not inside a form-row) |
| `.flex-1` | Utility: `flex: 1` |

### Imported via `@use 'details'`

| Class | Purpose |
|---|---|
| `.detail-header` | Drawer header: flex row, space-between |
| `.detail-title-group` | Leading icon + title-text column |
| `.detail-title-text` | Column of title row + subtitle |
| `.detail-subtitle` | Muted line below h2 |
| `.detail-panels` | Flex column of p-panel cards, 0.75 rem gap |
| `.document-list` | Document file list |
| `.document-row` | Single document row with hover |
| `.document-name` | Truncating filename |
| `.document-size` | Muted file size label |
| `.document-actions` | Flex row of doc action buttons |
| `.drop-zone` | Dashed drag-drop area |
| `.empty-block` | Italic muted empty state in panels |
