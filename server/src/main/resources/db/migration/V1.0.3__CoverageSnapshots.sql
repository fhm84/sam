-- ============================================================
-- Coverage Snapshots
-- Stores precomputed coverage scores for all sheets against
-- a given ensemble. Not audited — computed data, not user-authored.
-- ============================================================

create table coverage_snapshots
(
    id              uuid             not null,
    created         timestamp(6),
    lastUpdate      timestamp(6),
    version         integer          not null,
    ensemble_id     uuid             not null,
    sheet_id        uuid             not null,
    score           double precision not null,
    status          varchar(20)      not null check (status in ('COMPLETE', 'PLAYABLE', 'INCOMPLETE')),
    missingRequired boolean          not null,
    details         jsonb            not null,
    primary key (id)
);

create unique index coverage_snapshots_ensemble_sheet_idx on coverage_snapshots (ensemble_id, sheet_id);
create index coverage_snapshots_ensemble_idx on coverage_snapshots (ensemble_id);

alter table if exists coverage_snapshots
    add constraint fk_coverage_snapshots_ensemble
    foreign key (ensemble_id) references ensembles (id) on delete cascade;

alter table if exists coverage_snapshots
    add constraint fk_coverage_snapshots_sheet
    foreign key (sheet_id) references sheets (id) on delete cascade;
