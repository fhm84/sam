# ADR-0019: `cli` import is an additive sync, not skip-if-exists

**Status:** accepted

## Context

`cli import…` skipped every record that already existed on the target (sheets by title +
publisher, ensembles/persons/members by name, instruments by ID). Records with child records
are created in several requests — a sheet, then its parts; an ensemble, then voice by voice
and option by option — so a failure part-way left an incomplete record behind that every
rerun skipped. The first full import of a legacy catalogue hit exactly that: one sheet stayed
without parts. Skip-if-exists also can't apply a corrected conversion to an instance that
already holds an earlier import.

After go-live, the music librarian edits records in SAM, so an import file may be older than
the data it meets.

## Decision

Every import compares each file with the matching record on the target:

- **Matching:** by the file's `id` when it has one and that record exists (round trip from
  `cli export`), otherwise by the natural key as before.
- **Missing child records are added**: a sheet's parts (keyed like the database: instrument,
  part label, clef) and tags, an ensemble's voices (by label) and their instrument options.
  An interrupted import is completed by running it again.
- **Nothing is deleted.** Records and child records only on the target stay.
- **Differing fields are reported, not written**, unless `--update` is given. A field counts
  only when the file has a value for it: an empty field in the file means "unknown", never
  "clear". `--update` writes the file's values onto the target's current record, so fields
  the file leaves empty keep their value.
- **`--dry-run` reads the target** (read-only) and prints what would be created, added or
  changed; it needs the same access as an import.

Each file ends as `created`, `updated` (something was written), `unchanged` or `failed`; the
summary also counts records whose field differences were left alone.

## Consequences

- Rerunning an import is the way to repair a failed one; no manual cleanup in SAM.
- A dry run is no longer offline: it needs a reachable server and credentials.
- `--update` can overwrite edits made in SAM; the report of the plain run is the place to
  check that first.
- A child record whose own fields changed (an existing part's notes, an option's type) is not
  compared; only missing child records are added.
