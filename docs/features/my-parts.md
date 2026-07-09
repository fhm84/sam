# My Parts

A personalised, read-only view for authenticated musicians showing only the sheets that contain at least one instrumentation for their instrument(s).

## How it works

1. The server resolves the calling user's `userId` (OIDC `sub` claim) to a `Musician` record.
2. All `EnsembleMembership` rows for that musician are collected. The `instrument_id` of each non-null instrument membership is gathered into a set.
3. `GET /api/me/parts` returns sheets that contain **at least one** `Instrumentation` whose `instrument_id` is in that set — regardless of which ensemble voice the musician is assigned to.
4. Each sheet in the response carries a `myInstrumentations` field containing only the subset of that sheet's instrumentations that match the musician's instruments. Other instrumentations are not included.

**Matching is instrument-based, not voice-based.** A doubling musician (Bb Trumpet + Flugelhorn memberships) sees all Bb Trumpet and Flugelhorn instrumentations for every sheet. This is intentional for small ensembles where part assignment is flexible.

## Empty-state cases

| Situation | Response |
|---|---|
| User's OIDC `sub` does not match any `Musician.userId` | Empty list + hint to contact librarian |
| Musician is on the roster but has no instrument assignment (conductor-only) | Empty list |
| No sheets contain the musician's instruments | Empty list |

## Endpoint

`GET /api/me/parts?page=0&size=20` — paginated, sorted alphabetically by title.

## Angular UI

Route `/my-parts` — paginated table with columns: Sheet (title + subtitle, links to sheet detail), Composer, Genre, My Parts (instrument+partLabel chips for each matching instrumentation). Translated as "Meine Stimmen" in German.

## Related

- [ADR-0001](../architecture/decisions/adr-0001-musician-user-linking.md) — how logins map to musicians
- [Ensembles & Coverage](ensembles-coverage.md) — the memberships that drive matching
