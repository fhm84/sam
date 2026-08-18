# Collections & Setlists

Group sheets into named collections for organisation and concert planning.

## Types

| Type | Description |
|------|-------------|
| `FOLDER` | Static grouping of related pieces (e.g. "Christmas repertoire") |
| `SETLIST` | Ordered program for a specific concert or event, with an optional date |

The two-value enum is a deliberate decision — see [ADR-0006](../architecture/decisions/adr-0006-collection-type-enum.md).

## Metadata fields

| Field | Type | Notes |
|-------|------|-------|
| Name | String (required) | |
| Description | String | |
| Type | `FOLDER` or `SETLIST` | |
| Date | Date | For setlists (performance date) |
| Visibility | `WHOLE_ENSEMBLE` / `ADMINS_ONLY` / `PRIVATE` | Who can see the collection |
| Cover color | Hex string | Cover background in the collections overview |
| Cover image | FK → documents | Optional cover image |
| Ensemble | FK → ensembles (nullable) | Which ensemble the setlist belongs to; scopes coverage evaluation and the [AI setlist assistant](ai-setlist-assistant.md) |

## Collection items

An ordered list of items, each either:

- **SHEET** — links to a sheet, carries an **identifier** (position or label, e.g. `1`, `A1`, `Intro`)
- **TEXT** — a free-text programme block (e.g. the spoken introduction between pieces), optionally with an uploaded attachment. Draftable with AI — see [AI Setlist Assistant](ai-setlist-assistant.md).

Order is persisted server-side (a JPA-managed position, not just array order) and exposed on each item as a 0-based `orderNumber`. Drag-and-drop reordering in the UI calls `PUT /sheet-collections/{id}/items/order` with the full list of item IDs in the new order.

## Actions

- **Create / edit / delete** collections and individual items.
- Paginated sheet list within each collection.
- Sheet detail preview accessible from within the collection.
- **Export formats:** JSON, CSV, and ZIP (metadata + attached documents)
- **Table of contents:** Generate a PDF TOC from a collection/setlist (via `CollectionTocService`)
- **GEMA setlist:** Generate the GEMA reporting workbook (xlsx) for a setlist (via `GemaSetlistService`)
- **AI assistance:** Suggest repertoire pieces for a setlist goal and draft programme text — see [AI Setlist Assistant](ai-setlist-assistant.md)

## Related

- [AI Setlist Assistant](ai-setlist-assistant.md) — tool-grounded program suggestions and text drafting
- [Ensembles & Coverage](ensembles-coverage.md) — the ensemble link drives coverage-aware suggestions
- [Shares & Public Access](shares.md) — collections can be shared via public link
