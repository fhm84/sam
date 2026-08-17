# AI Setlist Assistant

Tool-grounded AI help for building a concert programme: suggests real repertoire pieces
for a free-text goal, and drafts the spoken introduction text for programme (TEXT) items.

## Program builder

`POST /sheet-collections/{id}/ai/suggest-items` — body: `{ goal }` (free text, e.g.
"90 minutes of festive music, start strong, calm middle section").

Returns `SetlistSuggestions` — a ranked list of `{ sheetId, title, composer, rationale }`.

Grounding rules:

- Suggestions come **only from the archive**: the LLM must call `SetlistCandidateTool`,
  which queries real sheets — it cannot invent pieces or IDs.
- Candidates are filtered by the linked ensemble's coverage (`COMPLETE`/`PLAYABLE` only),
  so only playable pieces are suggested. A collection without an `ensembleId` gets no
  candidates — the tool reports that playability cannot be evaluated.
- The ensemble is **never LLM-controlled**: a request-scoped context carries the
  already-authorized ensemble ID, so the tool exposes no ensemble argument to the model
  (see [ADR-0008](../architecture/decisions/adr-0008-assistant-ensemble-scoping.md)).

## Programme text drafting

`POST /sheet-collections/{cid}/items/{itemId}/ai/draft-text?language=de` — drafts the
spoken introduction for a TEXT item based on the neighboring piece's metadata. The
`language` query parameter (ISO 639-1, default `en`) is passed from the UI's active
locale. The announcer reviews and edits before use — the draft is never saved
automatically.

## UI

- **Assistant drawer** in the setlist editor: enter a goal, review ranked suggestions
  with rationale, add selected pieces as items.
- **Draft text** action on TEXT items: one click drafts the intro text in the current UI
  language into the item's editor.

## Access & logging

Both endpoints require the `music_librarian` or `admin` role. Each call is recorded in
the [event log](event-log.md) (`SETLIST_AI_SUGGESTION_GENERATED`,
`SETLIST_AI_TEXT_DRAFTED`) with token-usage attribution.

Prompts live as version-controlled resources under `server/src/main/resources/prompts/`
— deliberately not a live-editable config surface, since they carry the tool-grounding
rules (see [ADR-0009](../architecture/decisions/adr-0009-version-controlled-prompts.md)).

## Related

- [Collections & Setlists](collections.md) — the entities being assembled
- [Ensembles & Coverage](ensembles-coverage.md) — coverage filtering of candidates
- [AI Classification](ai-classification.md) / [AI Data Enrichment](ai-enrichment.md) — the other AI features
