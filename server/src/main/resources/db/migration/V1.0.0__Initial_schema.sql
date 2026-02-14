create table attachments
(
    id          uuid    not null,
    created     timestamp(6),
    lastUpdate  timestamp(6),
    version     integer not null,
    displayName varchar(255),
    type        varchar(255) check (type in
                                    ('FULL_SCORE', 'PART', 'COVER', 'LYRICS', 'MIDI', 'AUDIO', 'ANNOTATIONS', 'IMAGE',
                                     'ANALYSIS', 'TRANSCRIPTION', 'EXTERNAL_LINK', 'MUSIC_XML', 'OTHER',
                                     'UNSPECIFIED')),
    uploadedAt  timestamp(6),
    uploadedBy  varchar(255),
    document_id uuid,
    primary key (id)
);

create table attachments_AUD
(
    id          uuid    not null,
    REV         integer not null,
    REVTYPE     smallint,
    REVEND      integer,
    displayName varchar(255),
    type        varchar(255),
    uploadedAt  timestamp(6),
    uploadedBy  varchar(255),
    document_id uuid,
    primary key (REV, id)
);

create table classifications
(
    id            uuid    not null,
    created       timestamp(6),
    lastUpdate    timestamp(6),
    version       integer not null,
    status        varchar(20),
    attachment_id uuid,
    primary key (id)
);

create table classifications_AUD
(
    id            uuid    not null,
    REV           integer not null,
    REVTYPE       smallint,
    REVEND        integer,
    status        varchar(20),
    attachment_id uuid,
    primary key (REV, id)
);

create table collection_sheets
(
    id         uuid    not null,
    created    timestamp(6),
    lastUpdate timestamp(6),
    version    integer not null,
    identifier varchar(255),
    sheet_id   uuid,
    primary key (id)
);

create table collection_sheets_AUD
(
    id         uuid    not null,
    REV        integer not null,
    REVTYPE    smallint,
    REVEND     integer,
    identifier varchar(255),
    sheet_id   uuid,
    primary key (REV, id)
);

create table documents
(
    id         uuid         not null,
    created    timestamp(6),
    lastUpdate timestamp(6),
    version    integer      not null,
    filename   varchar(255) not null,
    mimeType   varchar(255),
    path       varchar(255) not null,
    refCount   integer      not null,
    sha256     varchar(64)  not null,
    size       bigint       not null,
    primary key (id)
);

create table documents_AUD
(
    id       uuid    not null,
    REV      integer not null,
    REVTYPE  smallint,
    REVEND   integer,
    filename varchar(255),
    mimeType varchar(255),
    path     varchar(255),
    refCount integer,
    sha256   varchar(64),
    size     bigint,
    primary key (REV, id)
);

create table genres
(
    id         uuid    not null,
    created    timestamp(6),
    lastUpdate timestamp(6),
    version    integer not null,
    name       varchar(255),
    primary key (id)
);

create table genres_AUD
(
    id      uuid    not null,
    REV     integer not null,
    REVTYPE smallint,
    REVEND  integer,
    name    varchar(255),
    primary key (REV, id)
);

create table instrumentations
(
    id             uuid    not null,
    created        timestamp(6),
    lastUpdate     timestamp(6),
    version        integer not null,
    clef           varchar(255) check (clef in ('TREBLE', 'ALTO', 'TENOR', 'BASS')),
    instrumentName varchar(255),
    notationType   varchar(255) check (notationType in
                                       ('STANDARD', 'TABLATURE', 'PERCUSSION', 'LEAD_SHEET', 'GRAPHIC')),
    notes          text,
    partLabel      varchar(255),
    transposition  varchar(255) check (transposition in ('C', 'D', 'Eb', 'F', 'G', 'A', 'Ab', 'Bb')),
    sheet_id       uuid,
    primary key (id)
);

create table instrumentations_attachments
(
    InstrumentationEntity_id uuid not null,
    attachments_id           uuid not null,
    primary key (InstrumentationEntity_id, attachments_id)
);

create table instrumentations_attachments_AUD
(
    REV                      integer not null,
    InstrumentationEntity_id uuid    not null,
    attachments_id           uuid    not null,
    REVTYPE                  smallint,
    REVEND                   integer,
    primary key (InstrumentationEntity_id, REV, attachments_id)
);

create table instrumentations_AUD
(
    id             uuid    not null,
    REV            integer not null,
    REVTYPE        smallint,
    REVEND         integer,
    clef           varchar(255),
    instrumentName varchar(255),
    notationType   varchar(255),
    notes          text,
    partLabel      varchar(255),
    transposition  varchar(255),
    sheet_id       uuid,
    primary key (REV, id)
);

create table musicians
(
    id         uuid    not null,
    created    timestamp(6),
    lastUpdate timestamp(6),
    version    integer not null,
    birthYear  integer,
    deathYear  integer,
    ipi        varchar(255),
    name       varchar(255),
    primary key (id)
);

create table musicians_AUD
(
    id        uuid    not null,
    REV       integer not null,
    REVTYPE   smallint,
    REVEND    integer,
    birthYear integer,
    deathYear integer,
    ipi       varchar(255),
    name      varchar(255),
    primary key (REV, id)
);

create table publishers
(
    id         uuid    not null,
    created    timestamp(6),
    lastUpdate timestamp(6),
    version    integer not null,
    address    varchar(255),
    contact    varchar(255),
    name       varchar(255),
    primary key (id)
);

create table publishers_AUD
(
    id      uuid    not null,
    REV     integer not null,
    REVTYPE smallint,
    REVEND  integer,
    address varchar(255),
    contact varchar(255),
    name    varchar(255),
    primary key (REV, id)
);

create table REVINFO
(
    REV      integer not null,
    REVTSTMP bigint,
    primary key (REV)
);

create table sheet_collections
(
    id          uuid    not null,
    created     timestamp(6),
    lastUpdate  timestamp(6),
    version     integer not null,
    date        date,
    description varchar(255),
    name        varchar(255),
    primary key (id)
);

create table sheet_collections_AUD
(
    id          uuid    not null,
    REV         integer not null,
    REVTYPE     smallint,
    REVEND      integer,
    date        date,
    description varchar(255),
    name        varchar(255),
    primary key (REV, id)
);

create table sheet_collections_collection_sheets
(
    SheetCollectionEntity_id uuid not null,
    sheets_id                uuid not null
);

create table sheet_collections_collection_sheets_AUD
(
    REV                      integer not null,
    SheetCollectionEntity_id uuid    not null,
    sheets_id                uuid    not null,
    REVTYPE                  smallint,
    REVEND                   integer,
    primary key (REV, SheetCollectionEntity_id, sheets_id)
);

create table sheets
(
    id                  uuid     not null,
    created             timestamp(6),
    lastUpdate          timestamp(6),
    version             integer  not null,
    additionalNotes     text,
    copyright           varchar(255),
    difficultyLevel     varchar(255),
    edition             varchar(255),
    fingerprint         char(64) NOT NULL,
    fingerprint_version integer  NOT NULL,
    gemaWorkNumber      varchar(255),
    iswc                varchar(255),
    originalBy          varchar(255),
    publisher           varchar(255),
    publisherIpi        varchar(255),
    rating              integer,
    subtitle            varchar(255),
    title               varchar(255),
    yearOfComposition   integer,
    arranger_id         uuid,
    composer_id         uuid,
    genre_id            uuid,
    primary key (id)
);

create table sheets_attachments
(
    SheetMusicEntity_id uuid not null,
    attachments_id      uuid not null,
    primary key (SheetMusicEntity_id, attachments_id)
);

create table sheets_attachments_AUD
(
    REV                 integer not null,
    SheetMusicEntity_id uuid    not null,
    attachments_id      uuid    not null,
    REVTYPE             smallint,
    REVEND              integer,
    primary key (REV, SheetMusicEntity_id, attachments_id)
);

create table sheets_AUD
(
    id                  uuid    not null,
    REV                 integer not null,
    REVTYPE             smallint,
    REVEND              integer,
    additionalNotes     text,
    copyright           varchar(255),
    difficultyLevel     varchar(255),
    edition             varchar(255),
    fingerprint         char(64),
    fingerprint_version integer,
    gemaWorkNumber      varchar(255),
    iswc                varchar(255),
    originalBy          varchar(255),
    publisher           varchar(255),
    publisherIpi        varchar(255),
    rating              integer,
    subtitle            varchar(255),
    title               varchar(255),
    yearOfComposition   integer,
    arranger_id         uuid,
    composer_id         uuid,
    genre_id            uuid,
    primary key (REV, id)
);

alter table if exists classifications
    add constraint UKr54vfq2u7uebx67lpm61a4m91 unique (attachment_id);

alter table if exists documents
    add constraint UKcfh78rjr0oxpk669kxbxg7c8i unique (sha256);

alter table if exists genres
    add constraint UKpe1a9woik1k97l87cieguyhh4 unique (name);

alter table if exists instrumentations
    add constraint uc_instrumentation unique (sheet_id, instrumentName, partLabel, transposition, clef);

alter table if exists instrumentations_attachments
    add constraint UKk6qbr4m70grnh6s0w0ejwr5na unique (attachments_id);

alter table if exists sheet_collections_collection_sheets
    add constraint UKbx6ahx5hc13dsbs2a9trmn8ss unique (sheets_id);

alter table if exists sheets_attachments
    add constraint UK6mh9jd4qul6qspg8f32l931gl unique (attachments_id);

create sequence REVINFO_SEQ start with 1 increment by 50;

alter table if exists attachments
    add constraint FKnpgwrxogx4rj1pynob5vd0n2d
        foreign key (document_id)
            references documents;

alter table if exists attachments_AUD
    add constraint FKno8tr4f12u2rfehm1461xqba2
        foreign key (REV)
            references REVINFO;

alter table if exists attachments_AUD
    add constraint FKrrd310jieacjov4nk5p4skenq
        foreign key (REVEND)
            references REVINFO;

alter table if exists classifications
    add constraint FK51x4bbmj47vcr9nmnuwmsmnv6
        foreign key (attachment_id)
            references attachments;

alter table if exists classifications_AUD
    add constraint FKjcmkjw30d57e9600fcuicjpgc
        foreign key (REV)
            references REVINFO;

alter table if exists classifications_AUD
    add constraint FKamha5gfuxwj8iu67xplg17mtn
        foreign key (REVEND)
            references REVINFO;

alter table if exists collection_sheets
    add constraint FKoainfuj8saxxuraksd4qrbsyv
        foreign key (sheet_id)
            references sheets;

alter table if exists collection_sheets_AUD
    add constraint FKmcragujry8co54oooacidsp2a
        foreign key (REV)
            references REVINFO;

alter table if exists collection_sheets_AUD
    add constraint FKc9ooc5fjpbhjvbbt0dimw25bn
        foreign key (REVEND)
            references REVINFO;

alter table if exists documents_AUD
    add constraint FKn4p16ydlata3fuhfej51d0q4a
        foreign key (REV)
            references REVINFO;

alter table if exists documents_AUD
    add constraint FKcelnyagqu491mfaw4apa51wwl
        foreign key (REVEND)
            references REVINFO;

alter table if exists genres_AUD
    add constraint FKu2wkbqf9o1r0v2n23ib4saa8
        foreign key (REV)
            references REVINFO;

alter table if exists genres_AUD
    add constraint FK2biwgekffc7yrf146o9nhnyqw
        foreign key (REVEND)
            references REVINFO;

alter table if exists instrumentations
    add constraint FK2trg3cgqowoc8l9ha9aqltfud
        foreign key (sheet_id)
            references sheets;

alter table if exists instrumentations_attachments
    add constraint FKiwnbx87wm92sk39urmtj5nq6v
        foreign key (attachments_id)
            references attachments;

alter table if exists instrumentations_attachments
    add constraint FKsl1ilhu1flbg9s721axnyaylr
        foreign key (InstrumentationEntity_id)
            references instrumentations;

alter table if exists instrumentations_attachments_AUD
    add constraint FKt92608y0sbrnhrc9cbk3563qo
        foreign key (REV)
            references REVINFO;

alter table if exists instrumentations_attachments_AUD
    add constraint FK8a3w9mtxn2w0mbuemtp84o9pr
        foreign key (REVEND)
            references REVINFO;

alter table if exists instrumentations_AUD
    add constraint FKrcxwj3y4t3f5e86vfqbmek8b2
        foreign key (REV)
            references REVINFO;

alter table if exists instrumentations_AUD
    add constraint FKory528udkntfljhtshg3g5iok
        foreign key (REVEND)
            references REVINFO;

alter table if exists musicians_AUD
    add constraint FK876qddpd1vyaw20vw671br8on
        foreign key (REV)
            references REVINFO;

alter table if exists musicians_AUD
    add constraint FKoxw5pir5jim35a07shdmtpywh
        foreign key (REVEND)
            references REVINFO;

alter table if exists publishers_AUD
    add constraint FKemn858m50iphesauhygwnr6an
        foreign key (REV)
            references REVINFO;

alter table if exists publishers_AUD
    add constraint FKhrv3yrytxbmqdwde38pigp9jc
        foreign key (REVEND)
            references REVINFO;

alter table if exists sheet_collections_AUD
    add constraint FKtfyveae5jv23qd6gpdlgl0t75
        foreign key (REV)
            references REVINFO;

alter table if exists sheet_collections_AUD
    add constraint FKny3skt873e29sjyxu7sx662b2
        foreign key (REVEND)
            references REVINFO;

alter table if exists sheet_collections_collection_sheets
    add constraint FKbho9wg6uckkb1vaodyor5v3cj
        foreign key (sheets_id)
            references collection_sheets;

alter table if exists sheet_collections_collection_sheets
    add constraint FKexrf9k7pmqnt6f6lktjd8g11d
        foreign key (SheetCollectionEntity_id)
            references sheet_collections;

alter table if exists sheet_collections_collection_sheets_AUD
    add constraint FKr0gq9ttny2xfs42a6min9a30e
        foreign key (REV)
            references REVINFO;

alter table if exists sheet_collections_collection_sheets_AUD
    add constraint FKguyoik9vmic8guweamok2sc9o
        foreign key (REVEND)
            references REVINFO;

alter table if exists sheets
    add constraint FKj842n7cjarxm49gekxa9qt67x
        foreign key (arranger_id)
            references musicians;

alter table if exists sheets
    add constraint FK6f2n9r0kx7yst0dwsx51xo0gt
        foreign key (composer_id)
            references musicians;

alter table if exists sheets
    add constraint FKgm7puj2ckqcbalxiwkehmli3t
        foreign key (genre_id)
            references genres;

alter table if exists sheets_attachments
    add constraint FK5pkvthou2jp3m2rydco8xhqs2
        foreign key (attachments_id)
            references attachments;

alter table if exists sheets_attachments
    add constraint FKkkx4isms1gffk7vpfx2b43l6o
        foreign key (SheetMusicEntity_id)
            references sheets;

alter table if exists sheets_attachments_AUD
    add constraint FK965si8jxphidr3j25d5lt6kum
        foreign key (REV)
            references REVINFO;

alter table if exists sheets_attachments_AUD
    add constraint FKjm5k8nkvmkas6dsvbo9pwgukp
        foreign key (REVEND)
            references REVINFO;

alter table if exists sheets_AUD
    add constraint FK3o2yrsqn8m7hawjlir4wb8g18
        foreign key (REV)
            references REVINFO;

alter table if exists sheets_AUD
    add constraint FK7wpgoyybd4ayuwkxbsmdl1uu9
        foreign key (REVEND)
            references REVINFO;

--------------------------------------------------------
-- set-up for google-like searching:
--    denormalized musician names
--    generated full-text
--    trigram fuzzy-search
--    phonetic (dmetaphone) fallback
--    indexes
--    ranked search query
--------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS fuzzystrmatch;

-- add denormalized search fields
ALTER TABLE sheets
    ADD COLUMN IF NOT EXISTS composer_name TEXT;
ALTER TABLE sheets
    ADD COLUMN IF NOT EXISTS arranger_name TEXT;
ALTER TABLE sheets
    ADD COLUMN IF NOT EXISTS composer_phonetic TEXT;
ALTER TABLE sheets
    ADD COLUMN IF NOT EXISTS arranger_phonetic TEXT;

-- synchronize musician names
CREATE OR REPLACE FUNCTION sync_sheet_musicians()
    RETURNS trigger AS
$$
BEGIN
    SELECT name
    INTO NEW.composer_name
    FROM musicians
    WHERE id = NEW.composer_id;

    SELECT name
    INTO NEW.arranger_name
    FROM musicians
    WHERE id = NEW.arranger_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER sheets_sync_musicians
    BEFORE INSERT OR UPDATE OF composer_id, arranger_id
    ON sheets
    FOR EACH ROW
EXECUTE FUNCTION sync_sheet_musicians();

-- on update of musician name
CREATE OR REPLACE FUNCTION propagate_musician_name()
    RETURNS trigger AS
$$
BEGIN
    UPDATE sheets
    SET composer_name = NEW.name
    WHERE composer_id = NEW.id;

    UPDATE sheets
    SET arranger_name = NEW.name
    WHERE arranger_id = NEW.id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER musician_name_update
    AFTER UPDATE OF name
    ON musicians
    FOR EACH ROW
EXECUTE FUNCTION propagate_musician_name();

-- phonetic code (DMetaphone)
CREATE OR REPLACE FUNCTION update_phonetics()
    RETURNS trigger AS
$$
BEGIN
    NEW.composer_phonetic :=
            dmetaphone(coalesce(NEW.composer_name, ''));

    NEW.arranger_phonetic :=
            dmetaphone(coalesce(NEW.arranger_name, ''));

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER sheets_update_phonetics
    BEFORE INSERT OR UPDATE OF composer_name, arranger_name
    ON sheets
    FOR EACH ROW
EXECUTE FUNCTION update_phonetics();

-- add dedicated column for full-text search vector
ALTER TABLE sheets
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', coalesce(title, '')), 'A')
            || setweight(to_tsvector('simple', coalesce(subtitle, '')), 'B')
            || setweight(to_tsvector('simple', coalesce(composer_name, '')), 'A')
            || setweight(to_tsvector('simple', coalesce(arranger_name, '')), 'C')
        ) STORED;

-- create indexes
-- full-text
CREATE INDEX sheets_search_vector_idx
    ON sheets USING GIN (search_vector);

-- trigram
CREATE INDEX sheets_title_trgm_idx
    ON sheets USING GIN (title gin_trgm_ops);

CREATE INDEX sheets_composer_trgm_idx
    ON sheets USING GIN (composer_name gin_trgm_ops);

-- phonetic
CREATE INDEX sheets_composer_phonetic_idx
    ON sheets (composer_phonetic);

CREATE INDEX sheets_arranger_phonetic_idx
    ON sheets (arranger_phonetic);
