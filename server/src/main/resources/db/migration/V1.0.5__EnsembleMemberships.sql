-- ============================================================
-- Members and Ensemble Memberships
-- A member is the profile of a person who belongs to the band
-- (ADR-0015); it shares the person's id and carries the OIDC
-- userId for auth linking. Ensemble memberships are a member's
-- seats in an ensemble with optional voice and instrument and a
-- role per seat.
-- ============================================================

create table members
(
    id         uuid    not null,
    created    timestamp(6),
    lastUpdate timestamp(6),
    version    integer not null,
    userId     varchar(255) unique,
    primary key (id)
);

alter table if exists members
    add constraint FK_members_person
        foreign key (id) references persons;

-- Envers audit table
create table members_aud
(
    id      uuid    not null,
    REV     integer not null,
    REVTYPE smallint,
    REVEND  integer,
    userId  varchar(255),
    primary key (REV, id)
);

alter table if exists members_aud
    add constraint FK_members_aud_rev
        foreign key (REV) references REVINFO;

alter table if exists members_aud
    add constraint FK_members_aud_revend
        foreign key (REVEND) references REVINFO;

create table ensemble_memberships
(
    id            uuid        not null,
    created       timestamp(6),
    lastUpdate    timestamp(6),
    version       integer     not null,
    member_id     uuid        not null,
    ensemble_id   uuid        not null,
    voice_id      uuid,
    instrument_id varchar(255),
    role          varchar(32) not null default 'MEMBER',
    primary key (id)
);

alter table if exists ensemble_memberships
    add constraint FK_ensemble_memberships_member
        foreign key (member_id) references members;

alter table if exists ensemble_memberships
    add constraint FK_ensemble_memberships_ensemble
        foreign key (ensemble_id) references ensembles;

alter table if exists ensemble_memberships
    add constraint FK_ensemble_memberships_voice
        foreign key (voice_id) references ensemble_voices;

alter table if exists ensemble_memberships
    add constraint FK_ensemble_memberships_instrument
        foreign key (instrument_id) references instruments;

-- Multiple voice seats per member/ensemble allowed (e.g. doubles on two instruments).
-- At most one seat without a voice (conductor/role-only entry) per member/ensemble.
create unique index ux_em_member_ensemble_voice
    on ensemble_memberships (member_id, ensemble_id, voice_id)
    where voice_id is not null;

create unique index ux_em_member_ensemble_no_voice
    on ensemble_memberships (member_id, ensemble_id)
    where voice_id is null;

-- Envers audit table
create table ensemble_memberships_aud
(
    id            uuid    not null,
    REV           integer not null,
    REVTYPE       smallint,
    REVEND        integer,
    created       timestamp(6),
    lastUpdate    timestamp(6),
    version       integer,
    member_id     uuid,
    ensemble_id   uuid,
    voice_id      uuid,
    instrument_id varchar(255),
    role          varchar(32),
    primary key (id, REV)
);

alter table if exists ensemble_memberships_aud
    add constraint FK_ensemble_memberships_aud_revinfo
        foreign key (REV) references REVINFO;
