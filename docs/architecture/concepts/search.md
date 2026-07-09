# Search Infrastructure (PostgreSQL)

Sheets have a generated `tsvector` column combining title, subtitle, composer, and arranger with weighted ranks (A/B/C). Three complementary search strategies:

1. **Full-text search** — `tsvector @@ tsquery` with `ts_rank` (weight: 0.70)
2. **Trigram similarity** — `pg_trgm` on title and composer (weight: 0.20 / 0.10)
3. **Phonetic fallback** — `dmetaphone` on composer/arranger names (weight: 0.05)

Denormalized `composer_name`/`arranger_name` columns are kept in sync via database triggers.

Required PostgreSQL extensions: `pg_trgm`, `fuzzystrmatch`. The schema lives in
`server/src/main/resources/db/migration/V1.0.0__Initial_schema.sql`.

Trigram similarity is also reused for entity candidate matching during
[AI classification](classification.md) (threshold 0.3, up to 5 candidates).

## Related

- [Data Model](../data-model.md) — the `sheets` table
- [Search & Discovery feature](../../features/search-discovery.md) — user-facing search behaviour
