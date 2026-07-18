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
