# Plan: Automatic Coverage Snapshot Invalidation

Roadmap: [Automatic coverage snapshot invalidation](../roadmap.md#automatic-coverage-snapshot-invalidation--planned)
· Concept: [Coverage evaluation](../architecture/concepts/coverage.md)

**Why now:** snapshots are only refreshed by an explicit
`POST /ensembles/{id}/coverage/compute`. After a sheet, instrumentation or ensemble voice
changes, filters, the Explore shelves and the setlist assistant work on stale coverage
data, and nothing tells the user.

## Current state

- `coverage_snapshots` holds one row per (ensemble, sheet): score, status,
  `missingRequired` and JSON details, with a unique index on `(ensemble_id, sheet_id)`.
- `CoverageSnapshotService.compute(ensembleId)` re-evaluates **all** sheets for one
  ensemble via `CoverageEvaluationService.evaluate(sheet, ensemble)` and upserts.
- There is no per-sheet recompute and no change hooks.

## Decision needed: open question #5

Proposed answer: **recompute automatically and per sheet.** Evaluating one sheet
against every ensemble is cheap (a handful of ensembles, tens of voices), so recompute
synchronously at the end of the write transaction instead of only marking rows stale.
Ensemble or voice changes affect every sheet, so they trigger the existing full
`compute(ensembleId)` asynchronously.

## Tasks

| # | Task | Status |
|---|------|--------|
| 1 | Add `CoverageSnapshotService.recomputeForSheet(UUID sheetId)`, which evaluates and upserts the sheet against every ensemble. | pending |
| 2 | Call it from the sheet and instrumentation write paths (create / update / delete instrumentation, physical condition change, sheet delete cascades). Prefer one CDI event (`SheetCoverageChanged`) observed with `@Observes(during = AFTER_SUCCESS)` over sprinkling calls everywhere. | pending |
| 3 | Fire a full async `compute(ensembleId)` when an ensemble voice, voice option or weight changes (`@ObservesAsync` or a managed executor). | pending |
| 4 | Recompute for a newly created ensemble once its voices exist (or on first voice add). | pending |
| 5 | Keep the manual compute endpoint as a "rebuild" fallback, and document that it is no longer needed in normal use. | pending |
| 6 | Tests: change an instrumentation and assert that the snapshot status changes without a manual compute; change a voice and assert the async recompute runs (await in test). | pending |
| 7 | Close open question #5 in the roadmap, and update `docs/architecture/concepts/coverage.md`. | pending |

## Key files

- `server/src/main/java/de/halbmann/sam/business/ensembles/controller/CoverageSnapshotService.java`
- `server/src/main/java/de/halbmann/sam/business/ensembles/controller/CoverageEvaluationService.java`
- `server/src/main/java/de/halbmann/sam/business/ensembles/boundary/CoverageSnapshotRepository.java`
- Instrumentation / sheet / ensemble-voice services under `server/src/main/java/de/halbmann/sam/business/`
