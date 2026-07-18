-- ============================================================
-- Create Sheet wizard fields
-- Adds pages-per-part to instrumentations and a free-text
-- source (provenance) field to sheets.
-- ============================================================

alter table instrumentations add column pages varchar(255);
alter table sheets add column source varchar(255);

-- Envers audit tables
alter table instrumentations_aud add column pages varchar(255);
alter table sheets_aud add column source varchar(255);
