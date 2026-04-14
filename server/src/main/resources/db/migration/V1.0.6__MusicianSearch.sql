-- ============================================================
-- Musician name search improvements:
--   trigram GIN index for prefix / fuzzy matching
--   phonetic column for dmetaphone fallback
-- ============================================================

-- Trigram index so similarity() / word_similarity() / ILIKE queries are fast
CREATE INDEX IF NOT EXISTS musicians_name_trgm_idx
    ON musicians USING GIN (name gin_trgm_ops);

-- Phonetic column (recomputed whenever name changes)
ALTER TABLE musicians
    ADD COLUMN IF NOT EXISTS name_phonetic TEXT;

-- Back-fill existing rows
UPDATE musicians
SET name_phonetic = dmetaphone(coalesce(name, ''));

-- Keep phonetic in sync
CREATE OR REPLACE FUNCTION sync_musician_phonetic()
    RETURNS trigger AS
$$
BEGIN
    NEW.name_phonetic := dmetaphone(coalesce(NEW.name, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER musicians_sync_phonetic
    BEFORE INSERT OR UPDATE OF name
    ON musicians
    FOR EACH ROW
EXECUTE FUNCTION sync_musician_phonetic();

-- Index for phonetic lookups
CREATE INDEX IF NOT EXISTS musicians_name_phonetic_idx
    ON musicians (name_phonetic);
