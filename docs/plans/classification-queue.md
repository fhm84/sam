# Plan: Classification Queue (Batch Inbox)

Roadmap: [Classification queue](../roadmap.md#classification-queue--planned)
· Depends on: [classification form enhancements](classification-enhancements.md)

**Status:** placeholder. The scope must be settled (open questions below) before any task
breakdown.

**Why:** the design (`Classify`) shows a batch inbox for working through many scans:
Pending / Skipped / Done tabs, progress with a time estimate, and pause/resume. It is a
large standalone feature with its own data model and UX.

## What the design shows

- Queue tabs Pending / Skipped / Done; items with filename, inferred subtitle and confidence.
- Progress bar: classified / skipped / remaining, estimated time.
- Pause/resume of the batch.
- Keyboard flow: J/K between items, Cmd+Enter to apply and go to the next.
- Three layouts: Split (rail + PDF viewer + form), Stack (card flip), Chat (agent).

## Open questions

1. Is the queue persisted server-side (survives a page refresh) or client-side only?
2. Can skipped items be re-processed later, or are they discarded?
3. Is the Chat layout in scope, or only Split/Stack?
4. Is queue state per user or per ensemble?
5. Only unclassified uploads, or can classified documents be re-queued (see re-classify in the enhancements plan)?

## Likely affected areas

- A queue entity, or a classification status on the document/staging entity.
- `EventType` additions for queue events.
- A new Angular route for the full classify workspace.
- Possibly SSE for live progress.
