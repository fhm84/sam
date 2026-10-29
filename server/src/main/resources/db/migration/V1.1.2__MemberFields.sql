-- Member profile: contact fields, membership status and leaving date, invite time, instruments

ALTER TABLE members ADD COLUMN email varchar(255);
ALTER TABLE members ADD COLUMN mobile varchar(100);
ALTER TABLE members ADD COLUMN notes text;
ALTER TABLE members ADD COLUMN membershipStatus varchar(32) not null default 'ACTIVE';
ALTER TABLE members ADD COLUMN leftAt date;
ALTER TABLE members ADD COLUMN lastInviteSentAt timestamp(6);

ALTER TABLE members_aud ADD COLUMN email varchar(255);
ALTER TABLE members_aud ADD COLUMN mobile varchar(100);
ALTER TABLE members_aud ADD COLUMN notes text;
ALTER TABLE members_aud ADD COLUMN membershipStatus varchar(32);
ALTER TABLE members_aud ADD COLUMN leftAt date;
ALTER TABLE members_aud ADD COLUMN lastInviteSentAt timestamp(6);

create table member_instruments
(
    id            uuid    not null,
    version       integer not null,
    created       timestamp(6),
    lastUpdate    timestamp(6),
    member_id     uuid         not null,
    instrument_id varchar(255) not null,
    is_primary    boolean      not null default false,
    primary key (id)
);

alter table if exists member_instruments
    add constraint FK_member_instruments_member
        foreign key (member_id) references members;

alter table if exists member_instruments
    add constraint FK_member_instruments_instrument
        foreign key (instrument_id) references instruments;

create unique index ux_member_instrument
    on member_instruments (member_id, instrument_id);

create table member_instruments_aud
(
    id            uuid    not null,
    REV           integer not null,
    REVTYPE       smallint,
    REVEND        integer,
    version       integer,
    created       timestamp(6),
    lastUpdate    timestamp(6),
    member_id     uuid,
    instrument_id varchar(255),
    is_primary    boolean,
    primary key (id, REV)
);

alter table if exists member_instruments_aud
    add constraint FK_member_instruments_aud_revinfo
        foreign key (REV) references REVINFO;
