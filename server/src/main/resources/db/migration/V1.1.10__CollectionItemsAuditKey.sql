-- Removing an item shifts the ones behind it. Envers audits each shift as a DEL at the old position
-- plus an ADD at the new one, in the same revision, so the position has to be part of the audit key.
-- V1.1.0 added items_order to the table but not to the key: such a removal failed on the duplicate.

-- Rows from before V1.1.0 have no position: open ones get the item's current one, the rest 0.
UPDATE sheet_collections_items_aud a
SET items_order = COALESCE(
        (SELECT i.items_order
         FROM sheet_collections_items i
         WHERE a.revend IS NULL
           AND i.sheet_collections_id = a.sheet_collections_id
           AND i.items_id = a.items_id),
        0)
WHERE a.items_order IS NULL;

ALTER TABLE sheet_collections_items_aud ALTER COLUMN items_order SET NOT NULL;

ALTER TABLE sheet_collections_items_aud DROP CONSTRAINT sheet_collections_collection_sheets_aud_pkey;
ALTER TABLE sheet_collections_items_aud
    ADD CONSTRAINT sheet_collections_items_aud_pkey
        PRIMARY KEY (rev, sheet_collections_id, items_id, items_order);
