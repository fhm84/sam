-- ============================================================
-- Ensemble Memberships
-- Links musicians to ensembles with optional voice and instrument
-- assignments. Also adds userId to musicians for OIDC auth linking.
-- ============================================================

alter table musicians add column user_id varchar(255) unique;

alter table musicians_aud add column user_id varchar(255);

create table ensemble_memberships (
    id uuid not null,
    created timestamp(6),
    lastUpdate timestamp(6),
    version integer not null,
    musician_id uuid not null,
    ensemble_id uuid not null,
    voice_id uuid,
    instrument_id varchar(255),
    conductor boolean not null default false,
    primary key (id)
);

alter table if exists ensemble_memberships
    add constraint FK_ensemble_memberships_musician
    foreign key (musician_id) references musicians;

alter table if exists ensemble_memberships
    add constraint FK_ensemble_memberships_ensemble
    foreign key (ensemble_id) references ensembles;

alter table if exists ensemble_memberships
    add constraint FK_ensemble_memberships_voice
    foreign key (voice_id) references ensemble_voices;

alter table if exists ensemble_memberships
    add constraint FK_ensemble_memberships_instrument
    foreign key (instrument_id) references instruments;

-- Multiple voice memberships per musician/ensemble allowed (e.g. doubles on two instruments).
-- At most one conductor/role-only entry per musician/ensemble (voice_id IS NULL).
create unique index ux_em_musician_ensemble_voice
    on ensemble_memberships (musician_id, ensemble_id, voice_id)
    where voice_id is not null;

create unique index ux_em_musician_ensemble_no_voice
    on ensemble_memberships (musician_id, ensemble_id)
    where voice_id is null;

-- Envers audit table
create table ensemble_memberships_aud (
    id uuid not null,
    rev integer not null,
    revtype smallint,
    created timestamp(6),
    lastUpdate timestamp(6),
    version integer,
    musician_id uuid,
    ensemble_id uuid,
    voice_id uuid,
    instrument_id varchar(255),
    conductor boolean,
    primary key (id, rev)
);

alter table if exists ensemble_memberships_aud
    add constraint FK_ensemble_memberships_aud_revinfo
    foreign key (rev) references revinfo;
