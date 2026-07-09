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

The sheets list supports both **table view** and **card view** (user preference). Each row/card shows coverage badges for all ensembles that have a computed snapshot.

## Related

- [Ensembles & Coverage](ensembles-coverage.md) — where the coverage badges come from
- [Roadmap](../roadmap.md#7-ux--discovery) — advanced combined search, more filter dimensions, bulk actions
