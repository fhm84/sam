-- ============================================================
-- Person name search improvements:
--   trigram GIN index for prefix / fuzzy matching
--   phonetic column for dmetaphone fallback
-- ============================================================

-- Trigram index so similarity() / word_similarity() / ILIKE queries are fast
CREATE INDEX IF NOT EXISTS persons_name_trgm_idx
    ON persons USING GIN (name gin_trgm_ops);

-- Phonetic column (recomputed whenever name changes)
ALTER TABLE persons
    ADD COLUMN IF NOT EXISTS name_phonetic TEXT;

-- Back-fill existing rows
UPDATE persons
SET name_phonetic = dmetaphone(coalesce(name, ''));

-- Keep phonetic in sync
CREATE OR REPLACE FUNCTION sync_person_phonetic()
    RETURNS trigger AS
$$
BEGIN
    NEW.name_phonetic := dmetaphone(coalesce(NEW.name, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER persons_sync_phonetic
    BEFORE INSERT OR UPDATE OF name
    ON persons
    FOR EACH ROW
EXECUTE FUNCTION sync_person_phonetic();

-- Index for phonetic lookups
CREATE INDEX IF NOT EXISTS persons_name_phonetic_idx
    ON persons (name_phonetic);
