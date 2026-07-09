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

## Collection sheets (membership)

Each entry in a collection links to a sheet and carries:
- **Identifier** — position or label within the collection (e.g. `1`, `A1`, `Intro`)

## Actions

- **Create / edit / delete** collections and individual sheet memberships.
- Paginated sheet list within each collection.
- Sheet detail preview accessible from within the collection.
- **Export formats:** JSON, CSV, and ZIP (metadata + attached documents)
- **Table of contents:** Generate a PDF TOC from a collection/setlist (via `CollectionTocService`)

## Related

- [Shares & Public Access](shares.md) — collections can be shared via public link
- [Roadmap](../roadmap.md#5-access-control--sharing) — visibility, cover color/image are planned
