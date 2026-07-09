# AI Classification

Two-step workflow that reads a document and pre-populates the archiving form.
Architectural background: [AI Classification concept](../architecture/concepts/classification.md).

## Step 1 — Classify (`POST /documents/{id}/classify`)

1. **Text extraction** (PDFBox) — if ≥ 50 printable characters are found in the PDF, the text path is used (fast, no vision API calls).
2. **Vision fallback** — scanned PDFs and images are converted to PNG and analysed by the configured LLM (Ollama / OpenAI / Vertex AI Gemini).
3. **Metadata extraction** — the AI returns: title, subtitle, publisher, composer, arranger, genre, year, edition, ISWC, instrument name, part label, clef, notation type.
4. **Entity pre-matching** against existing data:
   - Composer / arranger — trigram similarity (pg_trgm)
   - Sheet — exact title match
   - Instruments — trigram similarity (threshold 0.3, up to 5 ranked candidates)
5. A `SheetClassification` is returned containing the raw AI result, pre-matched entity IDs, and a pre-filled `ClassificationApplyRequest` ready for user review.

**Agentic mode** (opt-in via `sam.classification.agentic=true`) — a second AI pass using tool calls (`searchSheets`, `searchMusicians`, `searchInstruments`) resolves entity references autonomously before building the suggestion. Falls back to the standard suggestion on error.

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
