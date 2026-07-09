# Ensembles & Coverage Evaluation

SAM models ensembles as **Besetzungsrahmen** for amateur bands – focusing on playability rather than perfect theoretical coverage.

- **Ensemble definitions**
  Define named ensembles as collections of instrument groups (e.g. Trumpets, Saxophones, Tenorhorns), independent of piece-specific musical roles.

- **Ensemble voices**
  Each voice represents an instrument group and defines:
    - `required` — whether the group is mandatory for playability
    - `minCount` — minimum number of parts required to be playable
    - `targetCount` — ideal number of parts for good balance
    - `weight` — musical importance of the group
    - one or more `VoiceOption` entries (PRIMARY / ALTERNATE / FALLBACK), each specifying an instrument and a `factor` (1.0 = ideal, < 1.0 = substitute quality)
    - **A voice without options cannot be matched by any instrumentation**

- **Instrument matching (`MatchingService`)**
  A voice option matches an instrumentation only when the instrument IDs are identical (exact match). No automatic transposition or alias fallback — substitution must be modelled explicitly via ALTERNATE/FALLBACK options.
  Two secondary modifiers can reduce the score below 1.0:
    - *Clef factor* (0.7 for non-transposing instruments when a specific clef is set)
    - *Notation-type factor* (0.8 for percussion notation, 0.7 for tablature/graphic)
    - Scores below 0.3 are treated as no match.

- **Coverage evaluation**
  Voices are processed in priority order — required voices first, then by weight descending — so that the most important seats get first pick of available instrumentations. Each sheet instrumentation is claimed by at most one voice (greedy allocation).

  Per voice:
  ```
  effectiveCount = Σ (matchScore × option.factor)   for each claimed instrumentation
  normalized     = min(effectiveCount / targetCount, 1.0)
  countScore     = baseScore + (1 − baseScore) × normalized
  ```
  where `baseScore` (default **0.7**, configurable via `sam.coverage.base-score`) ensures any positive match immediately scores at least 70% of the voice’s contribution.

  Overall:
  ```
  coverageScore = Σ(countScore × voice.weight) / Σ(voice.weight)
  ```

  A required voice with `effectiveCount < minCount` marks the piece as not playable and contributes 0 to the score regardless of partial matches.

- **Status classification**

  | Status | Condition |
  |--------|-----------|
  | `INCOMPLETE` | One or more required voices are missing |
  | `PLAYABLE` | All required voices covered; `coverageScore < 0.85` |
  | `COMPLETE` | All required voices covered; `coverageScore ≥ 0.85` |

- **Coverage snapshots**
  Live evaluation is triggered on demand. Results are stored as `CoverageSnapshot` records (one per ensemble × sheet pair, upserted) so the sheets list can display coverage badges without per-request evaluation. Snapshots must be manually recomputed via `POST /api/ensembles/{id}/coverage/compute` — there is no automatic invalidation.

- **Coverage details**
  Evaluation results include a per-voice breakdown with effective part count, normalised score, and a human-readable explanation of which instrumentations were matched and via which option.

## Related

- [Data Model](../data-model.md) — ensemble/voice/option entities
- [Ensembles & Coverage feature](../../features/ensembles-coverage.md) — user-facing behaviour
- [Roadmap](../../roadmap.md) — automatic snapshot invalidation is planned
