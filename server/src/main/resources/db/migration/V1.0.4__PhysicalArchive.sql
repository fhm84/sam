-- ============================================================
-- Physical Archive
-- Adds physical archive location and condition to instrumentations
-- so ensemble members can find printed copies in the archive.
-- ============================================================

alter table instrumentations add column physical_location text;
alter table instrumentations add column physical_condition varchar(20)
    check (physical_condition in ('GOOD', 'WORN', 'DAMAGED', 'LOST'));

-- Envers audit tables
alter table instrumentations_aud add column physical_location text;
alter table instrumentations_aud add column physical_condition varchar(20);
