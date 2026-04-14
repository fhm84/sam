-- ============================================================
-- Physical Archive
-- Adds physical archive location and condition to instrumentations
-- so ensemble members can find printed copies in the archive.
-- ============================================================

alter table instrumentations add column physicallLocation text;
alter table instrumentations add column physicalCondition varchar(20)
    check (physicalCondition in ('GOOD', 'WORN', 'DAMAGED', 'LOST'));

-- Envers audit tables
alter table instrumentations_aud add column physicalLocation text;
alter table instrumentations_aud add column physicalCondition varchar(20);
