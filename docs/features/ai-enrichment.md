# AI Data Enrichment

Suggests missing or complementary metadata for an **existing** sheet, based on its current metadata (no document required).

## Endpoint

`POST /sheets/{id}/enrich` — analyses the sheet's title, composer, arranger, genre, and other known fields, then returns:

| Suggestion | Condition |
|-----------|-----------|
| Tags | Always suggested (new, not already in the tag set) |
| Style | Only when style is not yet set |
| Difficulty level | Only when difficulty is not yet set |
| Year of composition | Only when year is not yet set |
| Additional notes | Only when notes are empty |

## Enrichment dialog (UI)

- Opened via the **"Enrich with AI"** (sparkles) button in the sheet detail header.
- Loading spinner while the AI runs.
- Per-suggestion checkboxes — pre-checked, user can deselect individually.
- Tags shown as chip-style checkboxes.
- "Apply selected" calls `PUT /sheets/{id}` and reloads the sheet.
- "No suggestions" state when metadata is already complete.

## Related

- [AI Classification](ai-classification.md) — the document-based counterpart
- [Sheet Music](sheets.md) — the fields being enriched
