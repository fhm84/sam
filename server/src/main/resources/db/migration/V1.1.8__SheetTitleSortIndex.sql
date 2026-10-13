-- ============================================================
-- Sheet title sort index
-- The sheets list sorts by (title, id) by default; id is the
-- unique tiebreaker that keeps LIMIT/OFFSET paging stable.
-- A btree on exactly that key lets an unfiltered page read the
-- first rows in order instead of sorting the whole table.
-- (sheets_title_trgm_idx is a GIN trigram index for similarity
-- search and cannot serve ORDER BY.)
-- ============================================================

CREATE INDEX sheets_title_id_idx ON sheets (title, id);
