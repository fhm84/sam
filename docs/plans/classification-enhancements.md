# Plan: Classification Form Enhancements

Roadmap: [Classification form enhancements](../roadmap.md#classification-form-enhancements--planned)
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
| 1 | **Tags in apply request**: `tags` (List<String>) on `ClassificationApplyRequest`, applied to the created or matched sheet. | pending |
| 2 | **Notes in apply request**: `notes` (String, nullable), appended to the sheet's notes. | pending |
| 3 | **Document type**: `documentType` enum (`PART` / `SCORE` / `SOLO`) on `SheetAnalyzerResult`, surfaced in `SheetClassification` (prompt update in the versioned prompt files, see ADR-0009). | pending |
| 4 | **Field-level confidence**: per-field confidence (title, composer, arranger, instrument, …) as a structured record in `SheetClassification`. | pending |
| 5 | **Ranked alternatives**: `List<ScoredCandidate>` (id, name, score) for the sheet match and instrument match, instead of only the top hit. Trigram matching already produces up to 5 candidates. | pending |
| 6 | **Sanity checks**: warnings in the classify response (duplicate instrumentation, missing archive location, instrument mismatch). | pending |
| 7 | **Re-analyse**: `POST /documents/{id}/re-classify` re-runs AI on an already-classified document. | pending |
| 7a | **Instrument aliases in matching**: include `instrument_aliases` in instrument candidate matching (aliases are stored but unused by classification today). | pending |
| 8 | **Angular classify dialog**: confidence indicators, alternatives picker, warnings, tags + notes fields, document type selector. | pending |

## Key files

- `api/src/main/java/de/halbmann/sam/api/entity/classification/` (`SheetClassification`, `ClassificationApplyRequest`)
- `server/src/main/java/de/halbmann/sam/classification/boundary/` (`ClassificationService`, `SheetAnalyzer`)
- `server/src/main/java/de/halbmann/sam/classification/controller/DocumentClassificationService.java`
- `server/src/main/java/de/halbmann/sam/classification/entity/SheetAnalyzerResult.java`
- `ui/src/main/webui/src/app/features/uploads/` (classification dialog)
