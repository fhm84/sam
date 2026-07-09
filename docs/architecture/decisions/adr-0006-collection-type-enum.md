# ADR-0006: Keep `FOLDER`/`SETLIST` collection type enum

**Status:** accepted

## Context

Design mockups present richer collection categories (Concert, Season,
Rehearsal, Custom) than the data model's two-value `CollectionType` enum.

## Decision

The enum stays as-is: `FOLDER` (static grouping) and `SETLIST` (ordered,
dated programme). The UI maps design labels onto it: Concert → `SETLIST`;
Season / Rehearsal / Custom → `FOLDER`.

## Consequences

- No migration; export, TOC, and coverage logic keep a single distinction that
  actually changes behaviour (ordered + dated vs not).
- If a future feature needs to distinguish e.g. Season from Rehearsal folders,
  a display-category field can be added without touching the type semantics.

See [Collections & Setlists feature](../../features/collections.md) and the
collection visibility & cover entry in the [Roadmap](../../roadmap.md#5-access-control--sharing).
