-- Link a setlist/collection to the ensemble it belongs to (needed to evaluate coverage
-- against the right ensemble, e.g. for the AI setlist assistant).

ALTER TABLE sheet_collections ADD COLUMN ensembleId uuid;
ALTER TABLE sheet_collections_aud ADD COLUMN ensembleId uuid;

ALTER TABLE IF EXISTS sheet_collections
    ADD CONSTRAINT FK_sheet_collections_ensemble
        FOREIGN KEY (ensembleId) REFERENCES ensembles (id) ON DELETE SET NULL;
