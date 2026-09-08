-- ============================================================
-- Sheet musical and arranger-rights fields
-- Adds tempo (bpm), musical key, and arrangement-rights fields
-- to sheets. Also fixes the attachments.type CHECK constraint,
-- which never included PROGRAM_NOTE despite the enum and
-- SheetCollectionService.attachDocument() using it.
-- ============================================================

alter table sheets add column tempo integer;
alter table sheets add column tonality varchar(24);
alter table sheets add column arrangementPublisher varchar(255);
alter table sheets add column arrangementRightsUntil integer;

-- Envers audit tables
alter table sheets_aud add column tempo integer;
alter table sheets_aud add column tonality varchar(24);
alter table sheets_aud add column arrangementPublisher varchar(255);
alter table sheets_aud add column arrangementRightsUntil integer;

-- Fix: PROGRAM_NOTE was missing from the attachments.type CHECK constraint
alter table attachments drop constraint attachments_type_check;
alter table attachments add constraint attachments_type_check check (type in
    ('FULL_SCORE', 'PART', 'COVER', 'LYRICS', 'MIDI', 'AUDIO', 'ANNOTATIONS', 'IMAGE',
     'ANALYSIS', 'TRANSCRIPTION', 'EXTERNAL_LINK', 'MUSIC_XML', 'PROGRAM_NOTE', 'OTHER',
     'UNSPECIFIED'));
