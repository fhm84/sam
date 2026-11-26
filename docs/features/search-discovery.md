# Search & Discovery

Database internals: [Search Infrastructure concept](../architecture/concepts/search.md).

## Full-text search

`GET /sheets?q=<query>` uses a three-tier scoring strategy:

| Strategy | Technique | Weight |
|----------|-----------|--------|
| Full-text | PostgreSQL `tsvector` + `ts_rank` | 0.70 |
| Trigram | `pg_trgm` similarity on title and composer | 0.20 / 0.10 |
| Phonetic | `dmetaphone` on composer / arranger names | 0.05 |

Results are ranked by combined score. The UI debounces input and paginates results.

## Filter dimensions

| Filter | Description |
|--------|-------------|
| Genre | Dropdown (all distinct genres in the database) |
| Letter | A–Z browser by title first letter (optionally pre-filtered by genre) |
| Ensemble coverage | Filter by coverage status for a selected ensemble |
| Parts per instrument | Rows of `<instrument> <comparison> <number>`, all of which must hold (see below) |

### Part count filter

A sheet's instrumentation has one row per part, so "4 horns" means four rows for the horn
and there is no count column to filter on. The part count filter counts those rows instead:
each row in the filter panel picks an instrument, a comparison (`=`, `≠`, `<`, `≤`, `>`,
`≥`) and a number, and a sheet matches if all rows hold. That covers "exactly 4 horns"
(`= 4`), "no oboe" (`= 0`) and combinations like "4 horns, 2 clarinets, no oboe". A NOT
isn't needed, because each comparison's opposite is in the list too.

The API takes one repeatable `partCount` query parameter per row:

```
GET /sheets?partCount=HORN_F:EQ:4&partCount=OBOE:EQ:0
```

The value is `<instrumentId>:<comparison>:<parts>`, with comparison `EQ`, `NEQ`, `LT`,
`LTE`, `GT` or `GTE`. The instrument ID is everything before the last two colons, so it may
contain colons itself. The filter also applies to a full-text `q` search and to the
`totalCount`. A request fails with `400` for a malformed value, for more than 20 values, or
for an instrument ID that doesn't exist (it would have 0 parts everywhere, so `= 0` would
match every sheet). Each value becomes a correlated count subquery, which the
`(sheet_id, instrument_id, …)` unique index on `instrumentations` answers.

## Browse

The sheets list supports **table view**, **card view**, and **explore view** (user preference). Each row/card shows coverage badges for all ensembles that have a computed snapshot.

## Explore view

A query-less discovery mode on the Sheets Overview: horizontal shelves of curated
groupings, a frequency-weighted tag cloud (click-through to the tag-filtered card view),
and a random "surprise pick" with a shuffle button. Served by `GET /sheets/explore`
(combined shelves response) and `GET /sheets/explore/surprise`; both accept an optional
`ensemble` parameter that attaches coverage badges.

| Shelf | Logic |
|-------|-------|
| Needs attention | Coverage status `INCOMPLETE` for the selected ensemble (only shown when an ensemble is selected), worst score first |
| Crowd pleasers | Most appearances in dated `SETLIST` collections within the last 12 months (rolling) |
| Hidden gems | Never appeared in any setlist, longest-neglected first |
| Quick fillers | `duration` < 3:30 |
| Big finishes | `duration` ≥ 5:00 |
| Recently added | Created within the last 30 days |

## Related

- [Ensembles & Coverage](ensembles-coverage.md) — where the coverage badges come from
- [Roadmap](../roadmap.md#7-ux--discovery) — advanced combined search, more filter dimensions, bulk actions
