-- Instrument catalogue enrichment: family, defaultClef, catalog ordering, OCR aliases

ALTER TABLE instruments ADD COLUMN family varchar(32);
ALTER TABLE instruments ADD COLUMN defaultClef varchar(16);
ALTER TABLE instruments ADD COLUMN catalogSection varchar(255);
ALTER TABLE instruments ADD COLUMN catalogPosition integer;

ALTER TABLE instruments_aud ADD COLUMN family varchar(32);
ALTER TABLE instruments_aud ADD COLUMN defaultClef varchar(16);
ALTER TABLE instruments_aud ADD COLUMN catalogSection varchar(255);
ALTER TABLE instruments_aud ADD COLUMN catalogPosition integer;

create table instrument_aliases
(
    instrument_id uuid         not null references instruments (id) on delete cascade,
    alias         varchar(255) not null,
    alias_order   int          not null,
    primary key (instrument_id, alias_order)
);

create table instrument_aliases_AUD
(
    REV           integer      not null,
    instrument_id uuid         not null,
    alias         varchar(255) not null,
    alias_order   int          not null,
    REVTYPE       smallint,
    REVEND        integer,
    primary key (REV, instrument_id, alias_order)
);
