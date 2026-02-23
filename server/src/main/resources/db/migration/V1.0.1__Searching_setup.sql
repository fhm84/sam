--------------------------------------------------------
-- set-up for google-like searching:
--    denormalized musician names
--    generated full-text
--    trigram fuzzy-search
--    phonetic (dmetaphone) fallback
--    indexes
--    ranked search query
--------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS fuzzystrmatch;

-- add denormalized search fields
ALTER TABLE sheets
    ADD COLUMN IF NOT EXISTS composer_name TEXT;
ALTER TABLE sheets
    ADD COLUMN IF NOT EXISTS arranger_name TEXT;
ALTER TABLE sheets
    ADD COLUMN IF NOT EXISTS composer_phonetic TEXT;
ALTER TABLE sheets
    ADD COLUMN IF NOT EXISTS arranger_phonetic TEXT;

-- synchronize musician names
CREATE OR REPLACE FUNCTION sync_sheet_musicians()
    RETURNS trigger AS
$$
BEGIN
    SELECT name
    INTO NEW.composer_name
    FROM musicians
    WHERE id = NEW.composer_id;

    SELECT name
    INTO NEW.arranger_name
    FROM musicians
    WHERE id = NEW.arranger_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER sheets_sync_musicians
    BEFORE INSERT OR UPDATE OF composer_id, arranger_id
    ON sheets
    FOR EACH ROW
EXECUTE FUNCTION sync_sheet_musicians();

-- on update of musician name
CREATE OR REPLACE FUNCTION propagate_musician_name()
    RETURNS trigger AS
$$
BEGIN
    UPDATE sheets
    SET composer_name = NEW.name
    WHERE composer_id = NEW.id;

    UPDATE sheets
    SET arranger_name = NEW.name
    WHERE arranger_id = NEW.id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER musician_name_update
    AFTER UPDATE OF name
    ON musicians
    FOR EACH ROW
EXECUTE FUNCTION propagate_musician_name();

-- phonetic code (DMetaphone)
CREATE OR REPLACE FUNCTION update_phonetics()
    RETURNS trigger AS
$$
BEGIN
    NEW.composer_phonetic :=
            dmetaphone(coalesce(NEW.composer_name, ''));

    NEW.arranger_phonetic :=
            dmetaphone(coalesce(NEW.arranger_name, ''));

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER sheets_update_phonetics
    BEFORE INSERT OR UPDATE OF composer_name, arranger_name
    ON sheets
    FOR EACH ROW
EXECUTE FUNCTION update_phonetics();

-- add dedicated column for full-text search vector
ALTER TABLE sheets
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', coalesce(title, '')), 'A')
            || setweight(to_tsvector('simple', coalesce(subtitle, '')), 'B')
            || setweight(to_tsvector('simple', coalesce(composer_name, '')), 'A')
            || setweight(to_tsvector('simple', coalesce(arranger_name, '')), 'C')
        ) STORED;

-- create indexes
-- full-text
CREATE INDEX sheets_search_vector_idx
    ON sheets USING GIN (search_vector);

-- trigram
CREATE INDEX sheets_title_trgm_idx
    ON sheets USING GIN (title gin_trgm_ops);

CREATE INDEX sheets_composer_trgm_idx
    ON sheets USING GIN (composer_name gin_trgm_ops);

-- phonetic
CREATE INDEX sheets_composer_phonetic_idx
    ON sheets (composer_phonetic);

CREATE INDEX sheets_arranger_phonetic_idx
    ON sheets (arranger_phonetic);
