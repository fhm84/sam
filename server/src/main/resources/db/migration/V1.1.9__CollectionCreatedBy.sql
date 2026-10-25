-- Who created a collection (OIDC subject), so a PRIVATE collection can be the creator's own
-- draft. Existing collections stay empty: their PRIVATE ones remain visible to librarians and
-- admins, as before.

ALTER TABLE sheet_collections ADD COLUMN createdBy varchar(255);
ALTER TABLE sheet_collections_aud ADD COLUMN createdBy varchar(255);
