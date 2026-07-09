# AI-Powered Sheet Classification

LangChain4j with `@RegisterAiService` provides AI-based classification of uploaded sheet music. The workflow is a two-step process. Key classes: `server/src/main/java/de/halbmann/sam/classification/`.

## Step 1 — Classify (`POST /documents/{id}/classify`)

1. **Text extraction (PDFBox)** — For native PDFs, text is extracted first (fast, free). If fewer than 50 printable characters are found, the document is treated as scanned/image-only.
2. **Vision fallback** — Scanned PDFs and images are converted to PNG and sent to an LLM (Ollama, OpenAI, or Vertex AI Gemini — configurable) using the vision API.
3. **Structured metadata extraction** — A `SheetAnalyzerResult` is returned containing title, composer, arranger, genre, year, instrumentation details, etc.
4. **Entity pre-matching** — Composer and arranger names are matched against existing musicians; existing sheets are matched by exact title; instruments are matched via pg_trgm trigram similarity (threshold 0.3, up to 5 candidates with scores).
5. **Pre-filled suggestion (Option A)** — A `ClassificationApplyRequest` is built automatically from the AI result and best-match candidates. This is returned to the frontend for user review.
6. **Agentic resolution (Option B, opt-in)** — When `sam.classification.agentic=true`, a second AI pass using `ClassificationAgent` autonomously resolves entity references via tool calls (`searchSheets`, `searchMusicians`, `searchInstruments`) before returning the suggestion. Falls back to Option A on error.

## Step 2 — Apply (`POST /documents/{id}/apply`)

The reviewed `ClassificationApplyRequest` is submitted. The service:
- Resolves or creates the sheet, composer, arranger, and instrument entities
- Creates an `InstrumentationEntity` if instrument information was provided
- Attaches the document as an `AttachmentEntity` to the instrumentation (or directly to the sheet)

This is used to pre-populate and confirm the archiving workflow with minimal manual input.

## Related

- [AI Classification feature](../../features/ai-classification.md) — user-facing workflow incl. the review dialog
- [Search](search.md) — the trigram matching reused for candidate lookup
- [Flow 1 — AI-assisted archival](../runtime/README.md) — end-to-end sequence diagram
- [Roadmap](../../roadmap.md) — classification queue and form enhancements are planned
