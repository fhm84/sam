-- Musician profile enrichment: contact fields, status/role enums, linked instruments

ALTER TABLE musicians ADD COLUMN email varchar(255);
ALTER TABLE musicians ADD COLUMN mobile varchar(100);
ALTER TABLE musicians ADD COLUMN notes text;
ALTER TABLE musicians ADD COLUMN status varchar(32);
ALTER TABLE musicians ADD COLUMN role varchar(32);
ALTER TABLE musicians ADD COLUMN lastInviteSentAt timestamp(6);

ALTER TABLE musicians_aud ADD COLUMN email varchar(255);
ALTER TABLE musicians_aud ADD COLUMN mobile varchar(100);
ALTER TABLE musicians_aud ADD COLUMN notes text;
ALTER TABLE musicians_aud ADD COLUMN status varchar(32);
ALTER TABLE musicians_aud ADD COLUMN role varchar(32);
ALTER TABLE musicians_aud ADD COLUMN lastInviteSentAt timestamp(6);

create table musician_instruments
(
    id            uuid    not null,
    version       integer not null,
    created       timestamp(6),
    lastUpdate    timestamp(6),
    musician_id   uuid         not null,
    instrument_id varchar(255) not null,
    is_primary    boolean      not null default false,
    primary key (id)
);

alter table if exists musician_instruments
    add constraint FK_musician_instruments_musician
        foreign key (musician_id) references musicians;

alter table if exists musician_instruments
    add constraint FK_musician_instruments_instrument
        foreign key (instrument_id) references instruments;

create unique index ux_musician_instrument
    on musician_instruments (musician_id, instrument_id);

create table musician_instruments_aud
(
    id            uuid    not null,
    REV           integer not null,
    REVTYPE       smallint,
    REVEND        integer,
    version       integer,
    created       timestamp(6),
    lastUpdate    timestamp(6),
    musician_id   uuid,
    instrument_id varchar(255),
    is_primary    boolean,
    primary key (id, REV)
);

alter table if exists musician_instruments_aud
    add constraint FK_musician_instruments_aud_revinfo
        foreign key (REV) references REVINFO;
