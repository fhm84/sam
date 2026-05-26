-- Collection enrichment: visibility, cover color, and cover image

ALTER TABLE sheet_collections ADD COLUMN visibility varchar(32);
ALTER TABLE sheet_collections ADD COLUMN coverColor varchar(32);
ALTER TABLE sheet_collections ADD COLUMN coverImageId uuid;

ALTER TABLE sheet_collections_aud ADD COLUMN visibility varchar(32);
ALTER TABLE sheet_collections_aud ADD COLUMN coverColor varchar(32);
ALTER TABLE sheet_collections_aud ADD COLUMN coverImageId uuid;

ALTER TABLE IF EXISTS sheet_collections
    ADD CONSTRAINT FK_sheet_collections_cover_image
        FOREIGN KEY (coverImageId) REFERENCES documents (id) ON DELETE SET NULL;
