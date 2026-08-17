-- ============================================================
-- Sheet rights fields
-- Adds a rights status (archiving/digitization/distribution) and
-- a GEMA-reportable flag to sheets.
-- ============================================================

alter table sheets add column rightsStatus varchar(32);
alter table sheets add column gemaPflichtig varchar(16);

-- Envers audit tables
alter table sheets_aud add column rightsStatus varchar(32);
alter table sheets_aud add column gemaPflichtig varchar(16);
