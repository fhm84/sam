# Instruments

A canonical instrument catalogue used across instrumentations and ensemble voice definitions.

## Metadata fields

| Field | Type | Notes |
|-------|------|-------|
| ID | String (slug) | Unique, immutable key, e.g. `trumpet-bb` |
| Name | String (required) | Canonical name, e.g. "Bb Trumpet" |
| Display name | String | Short name shown in UI |
| Transposition | Enum | Concert pitch key: `C` · `Bb` · `Eb` · `F` · `Ab` · `D` · `A` · `G` |
| Family | Enum | `BRASS` · `WOODWIND` · `STRING` · `PERCUSSION` · `KEYBOARD` · `VOICE` · `OTHER` |
| Default clef | Enum | Instrument-level default (`Clef`). Instrumentations can still override it |
| Aliases | List of strings | Alternative spellings / abbreviations (e.g. "Flh.", "Flügelhorn"). Ordered and audited. Not yet used by classification matching (see [plan](../plans/classification-enhancements.md)) |
| Catalogue section / position | String / Integer | Drives catalogue ordering, e.g. "Brass · High" / #7 |

## Actions

- **Create / edit / delete** via a dialog.
- Paginated list with search by name and filter by transposition.

## Related

- [Instrumentations](instrumentations.md) — where instruments are referenced per sheet
- [Ensembles & Coverage](ensembles-coverage.md) — voice options reference instruments
