# Plan: Coverage Breakdown Enhancements

Roadmap: [Coverage breakdown enhancements](../roadmap.md#coverage-breakdown-enhancements--planned)
· Feature: [Ensembles & coverage](../features/ensembles-coverage.md)
· Concept: [Coverage evaluation](../architecture/concepts/coverage.md)

**Why:** the coverage engine already computes good scores, but the design (`Coverage
Breakdown`) shows richer annotations and two aggregate views that don't exist yet.

## Tasks

| # | Task | Status |
|---|------|--------|
| 1 | **Condition/substitute annotations**: expose `conditionPenalty` and `substituteFactor` as named fields on `VoiceCoverageDetail` (already computed internally). | pending |
| 2 | **Multi-ensemble view**: make `ensemble` optional on `GET /sheets/{id}/coverage`, returning results for all ensembles when omitted (side-by-side panel). | pending |
| 3 | **Ensemble gap report**: `GET /ensembles/{id}/gaps`, the per-voice count of sheets missing that voice, sorted by prevalence. Shared with the [home dashboard](home-dashboard.md). | pending |
| 4 | **Recommendations**: which 1–2 voices, if added, would move the most sheets from INCOMPLETE to PLAYABLE. | pending |
| 5 | **Angular**: penalty labels, multi-ensemble panel and recommendations in the sheet detail coverage view. | pending |

## Key files

- `api/src/main/java/de/halbmann/sam/api/entity/ensembles/VoiceCoverageDetail.java`
- `api/src/main/java/de/halbmann/sam/api/boundary/SheetsResource.java`, `EnsemblesResource.java`
- `server/src/main/java/de/halbmann/sam/business/ensembles/controller/` (`CoverageEvaluationService`, `CoverageSnapshotService`)
- `ui/src/main/webui/src/app/features/sheets/` (`sheet-detail.*`)
