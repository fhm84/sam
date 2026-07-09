# Instrumentations

Each [sheet](sheets.md) can have multiple instrumentations — one per instrument part.

## Metadata fields

| Field | Type | Notes |
|-------|------|-------|
| Instrument | Instrument reference | From the [instruments](instruments.md) catalogue |
| Part label | String | Distinguishes parts, e.g. "1st Trumpet", "Solo", "2. Bass" |
| Clef | Enum | `TREBLE` · `ALTO` · `TENOR` · `BASS` |
| Notation type | Enum | `STANDARD` · `TABLATURE` · `PERCUSSION` · `LEAD_SHEET` · `GRAPHIC` |
| Notes | Free text | Part-specific performance notes |
| Archive location | String | Physical storage location of the printed copy (e.g. "Cabinet A / Shelf 3 / Folder 12") |
| Physical condition | Enum | `GOOD` · `WORN` · `DAMAGED` · `LOST` — state of the printed copy |

## Actions

- **Create / edit / delete** via a dialog within the sheet detail view.
- **Bulk creation** — multiple instrumentations can be added in a single request.
- Each instrumentation has its own **document attachments** (individual part files) — see [Documents & Attachments](documents.md).

## Related

- [Ensembles & Coverage](ensembles-coverage.md) — instrumentations are matched against ensemble voices
- [Shares & Public Access](shares.md) — a single instrumentation can be shared via link
