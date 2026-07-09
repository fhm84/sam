# ADR-0005: Keep single `name` field on Musician

**Status:** accepted

## Context

The musician profile enrichment plan raised the question of splitting
`Musician.name` into first/last name fields (useful for sorting and form
conventions).

## Decision

`name` stays a single full-name string. The catalogue mixes ensemble members,
historical composers ("Wolfgang Amadeus Mozart"), and bands/artists where a
first/last split is artificial or wrong.

## Consequences

- No lossy migration of existing data; AI classification results (free-form
  names) map directly onto the field.
- Sorting by last name is not reliably possible — accepted for catalogue sizes
  in the hundreds.

See [Musicians feature](../../features/musicians.md) and the musician
profile enrichment entry in the [Roadmap](../../roadmap.md#3-musician-facing).
