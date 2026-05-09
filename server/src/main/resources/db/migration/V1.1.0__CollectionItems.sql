-- Rename collection_sheets to collection_items and introduce item_type discriminator

ALTER TABLE collection_sheets RENAME TO collection_items;

ALTER TABLE collection_items ADD COLUMN item_type varchar(255);
UPDATE collection_items SET item_type = 'SHEET';
ALTER TABLE collection_items ALTER COLUMN item_type SET NOT NULL;

ALTER TABLE collection_items ADD COLUMN textContent text;

-- Optional single-file attachment for TEXT-type items
ALTER TABLE collection_items
    ADD COLUMN attachment_id uuid REFERENCES attachments (id) ON DELETE SET NULL;

-- Rename join table, columns, and add 0-based ordering column
ALTER TABLE sheet_collections_collection_sheets RENAME TO sheet_collections_items;
ALTER TABLE sheet_collections_items RENAME COLUMN sheets_id TO items_id;
ALTER TABLE sheet_collections_items RENAME COLUMN sheetcollectionentity_id TO sheet_collections_id;

ALTER TABLE sheet_collections_items ADD COLUMN items_order int;

UPDATE sheet_collections_items s
SET items_order = sub.rn
FROM (
    SELECT ctid,
           (ROW_NUMBER() OVER (PARTITION BY sheet_collections_id ORDER BY ctid) - 1)::int AS rn
    FROM sheet_collections_items
) sub
WHERE s.ctid = sub.ctid;

ALTER TABLE sheet_collections_items ALTER COLUMN items_order SET NOT NULL;

-- Audit tables
ALTER TABLE collection_sheets_aud RENAME TO collection_items_aud;
ALTER TABLE collection_items_aud ADD COLUMN item_type varchar(255);
ALTER TABLE collection_items_aud ADD COLUMN textContent text;
ALTER TABLE collection_items_aud ADD COLUMN attachment_id uuid;

ALTER TABLE sheet_collections_collection_sheets_aud RENAME TO sheet_collections_items_aud;
ALTER TABLE sheet_collections_items_aud RENAME COLUMN sheets_id TO items_id;
ALTER TABLE sheet_collections_items_aud RENAME COLUMN sheetcollectionentity_id TO sheet_collections_id;
ALTER TABLE sheet_collections_items_aud ADD COLUMN items_order int;
