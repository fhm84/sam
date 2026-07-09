# AI Classification

Two-step workflow that reads a document and pre-populates the archiving form.
Architectural background: [AI Classification concept](../architecture/concepts/classification.md).

## Step 1 — Classify (`POST /documents/{id}/classify`)

The document is analysed by AI and matched against existing data. The AI detects:
title, subtitle, publisher, composer, arranger, genre, year, edition, ISWC,
instrument name, part label, clef, notation type. Detected composers/arrangers,
sheets, and instruments are pre-matched against the catalogue, with instruments
returned as ranked candidates.

The response is a `SheetClassification` containing the raw AI result, pre-matched
entity IDs, and a pre-filled `ClassificationApplyRequest` ready for user review.

Pipeline internals — the text-vs-vision decision, matching thresholds, and the
opt-in agentic mode — are documented once in the
[AI Classification concept](../architecture/concepts/classification.md).

## Step 2 — Apply (`POST /documents/{id}/apply`)

The user reviews and adjusts the pre-filled form, then submits a `ClassificationApplyRequest`. The server:

- Resolves or **creates** the sheet, composer, arranger, and instrument as needed.
- Creates a new `Instrumentation` if instrument details are provided.
- Attaches the document to the instrumentation (or directly to the sheet).

## Classification dialog (UI)

- Split-panel view: document preview on the left, review form on the right.
- Mode toggles for each entity: *use existing* · *create new* · *none*.
- Instrument candidates shown as a scored dropdown.
- Error state with retry option.

## Related

- [AI Data Enrichment](ai-enrichment.md) — metadata suggestions without a document
- [Roadmap](../roadmap.md#6-operational--integration) — classification queue and form enhancements are planned
