-- ============================================================
-- Ensembles, Ensemble Voices, Voice Options
-- ============================================================

create table ensembles
(
    id          uuid         not null,
    created     timestamp(6),
    lastUpdate  timestamp(6),
    version     integer      not null,
    name        varchar(255) not null,
    description text,
    primary key (id)
);

create table ensembles_AUD
(
    id          uuid    not null,
    REV         integer not null,
    REVTYPE     smallint,
    REVEND      integer,
    name        varchar(255),
    description text,
    primary key (REV, id)
);

create table ensemble_voices
(
    id          uuid             not null,
    created     timestamp(6),
    lastUpdate  timestamp(6),
    version     integer          not null,
    label       varchar(255)     not null,
    weight      double precision not null,
    required    boolean          not null,
    minCount    integer,
    targetCount integer,
    maxCount    integer,
    ensemble_id uuid             not null,
    primary key (id)
);

create table ensemble_voices_AUD
(
    id          uuid    not null,
    REV         integer not null,
    REVTYPE     smallint,
    REVEND      integer,
    label       varchar(255),
    weight      double precision,
    required    boolean,
    minCount    integer,
    targetCount integer,
    maxCount    integer,
    ensemble_id uuid,
    primary key (REV, id)
);

create table voice_options
(
    id            uuid             not null,
    created       timestamp(6),
    lastUpdate    timestamp(6),
    version       integer          not null,
    type          varchar(255) check (type in ('PRIMARY', 'ALTERNATE', 'FALLBACK')),
    factor        double precision not null,
    voice_id      uuid             not null,
    instrument_id varchar(255)     not null,
    primary key (id)
);

create table voice_options_AUD
(
    id            uuid    not null,
    REV           integer not null,
    REVTYPE       smallint,
    REVEND        integer,
    type          varchar(255),
    factor        double precision,
    voice_id      uuid,
    instrument_id varchar(255),
    primary key (REV, id)
);

-- Foreign keys for ensembles audit
alter table if exists ensembles_AUD
    add constraint FK_ensembles_AUD_REV
    foreign key (REV) references REVINFO;

alter table if exists ensembles_AUD
    add constraint FK_ensembles_AUD_REVEND
    foreign key (REVEND) references REVINFO;

-- Foreign keys for ensemble_voices
alter table if exists ensemble_voices
    add constraint FK_ensemble_voices_ensemble
    foreign key (ensemble_id) references ensembles;

-- Foreign keys for ensemble_voices audit
alter table if exists ensemble_voices_AUD
    add constraint FK_ensemble_voices_AUD_REV
    foreign key (REV) references REVINFO;

alter table if exists ensemble_voices_AUD
    add constraint FK_ensemble_voices_AUD_REVEND
    foreign key (REVEND) references REVINFO;

-- Foreign keys for voice_options
alter table if exists voice_options
    add constraint FK_voice_options_voice
    foreign key (voice_id) references ensemble_voices;

alter table if exists voice_options
    add constraint FK_voice_options_instrument
    foreign key (instrument_id) references instruments (id);

-- Foreign keys for voice_options audit
alter table if exists voice_options_AUD
    add constraint FK_voice_options_AUD_REV
    foreign key (REV) references REVINFO;

alter table if exists voice_options_AUD
    add constraint FK_voice_options_AUD_REVEND
    foreign key (REVEND) references REVINFO;
