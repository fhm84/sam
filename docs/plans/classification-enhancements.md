# Plan: Classification Form Enhancements

Roadmap: [Classification form enhancements](../roadmap.md#classification-form-enhancements--planned)
· Tracking issue: [#172](https://github.com/fhm84/sam/issues/172) (milestone [Everyday use](https://github.com/fhm84/sam/milestone/2))
· Feature: [AI classification](../features/ai-classification.md)
· Concept: [Classification](../architecture/concepts/classification.md)

**Why:** the two-step classify/apply workflow works, but the design shows richer AI
output (per-field confidence, ranked alternatives) and apply fields (tags, notes,
document type) that the DTOs don't carry. This pays off most during the initial
digitisation push, when many scans go through the form.

Queue and batch management is a separate plan: [classification queue](classification-queue.md).

## Tasks

| # | Task | Status |
|---|------|--------|
| 1 | **Tags in apply request**: `tags` (List<String>) on `ClassificationApplyRequest`, applied to the created or matched sheet. | [#173](https://github.com/fhm84/sam/issues/173) |
| 2 | **Notes in apply request**: `notes` (String, nullable), appended to the sheet's notes. | [#174](https://github.com/fhm84/sam/issues/174) |
| 3 | **Document type**: `documentType` enum (`PART` / `SCORE` / `SOLO`) on `SheetAnalyzerResult`, surfaced in `SheetClassification` (prompt update in the versioned prompt files, see ADR-0009). | [#175](https://github.com/fhm84/sam/issues/175) |
| 4 | **Field-level confidence**: per-field confidence (title, composer, arranger, instrument, …) as a structured record in `SheetClassification`. | [#176](https://github.com/fhm84/sam/issues/176) |
| 5 | **Ranked alternatives**: `List<ScoredCandidate>` (id, name, score) for the sheet match and instrument match, instead of only the top hit. Trigram matching already produces up to 5 candidates. | [#177](https://github.com/fhm84/sam/issues/177) |
| 6 | **Sanity checks**: warnings in the classify response (duplicate instrumentation, missing archive location, instrument mismatch). | [#178](https://github.com/fhm84/sam/issues/178) |
| 7 | **Re-analyse**: `POST /documents/{id}/re-classify` re-runs AI on an already-classified document. | [#179](https://github.com/fhm84/sam/issues/179) |
| 7a | **Instrument aliases in matching**: include `instrument_aliases` in instrument candidate matching (aliases are stored but unused by classification today). | [#180](https://github.com/fhm84/sam/issues/180) |
| 8 | **Angular classify dialog**: confidence indicators, alternatives picker, warnings, tags + notes fields, document type selector. | [#181](https://github.com/fhm84/sam/issues/181) |

## Key files

- `api/src/main/java/de/halbmann/sam/api/entity/classification/` (`SheetClassification`, `ClassificationApplyRequest`)
- `server/src/main/java/de/halbmann/sam/classification/boundary/` (`ClassificationService`, `SheetAnalyzer`)
- `server/src/main/java/de/halbmann/sam/classification/controller/DocumentClassificationService.java`
- `server/src/main/java/de/halbmann/sam/classification/entity/SheetAnalyzerResult.java`
- `ui/src/main/webui/src/app/features/uploads/` (classification dialog)
