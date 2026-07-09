# Ensembles & Coverage

Model the target instrumentation of a band, then evaluate how well a piece is covered.
Scoring internals: [Coverage Evaluation concept](../architecture/concepts/coverage.md).

## Ensemble definition

An ensemble represents a band configuration (e.g. "Brass Quintet", "20-piece Blaskapelle").

### Voices

Each ensemble has a list of **voices** (instrument groups):

| Field | Description |
|-------|-------------|
| Label | Display name (e.g. "1. Trumpet", "Tenorhorn", "Tuba") |
| Required | Whether the voice must be covered for the piece to be playable |
| Min count | Minimum number of parts for playability |
| Target count | Ideal number of parts |
| Weight | Musical importance for scoring (higher = more impact on overall score) |

### Voice options

Each voice has one or more instrument options modelling substitution:

| Type | Description |
|------|-------------|
| `PRIMARY` | Ideal instrument for this voice |
| `ALTERNATE` | Accepted substitute |
| `FALLBACK` | Last resort |

Each option has a **factor** (0.0–1.0): 1.0 = ideal, lower values downweight substitute quality.

## Coverage evaluation

`GET /sheets/{id}/coverage?ensemble={ensembleId}` evaluates the piece on-demand and
returns an overall coverage score, a status, and a per-voice breakdown showing which
instrumentations were matched, the effective part count, and a human-readable
explanation.

How the score is computed — voice priority, greedy instrumentation allocation, the
base score, clef/notation modifiers, and the exact status thresholds — is documented
once in the [Coverage Evaluation concept](../architecture/concepts/coverage.md).

**Status classification:**

| Status | Meaning |
|--------|---------|
| `INCOMPLETE` | One or more required voices are not sufficiently covered — not playable |
| `PLAYABLE` | All required voices covered, but overall coverage is still thin |
| `COMPLETE` | All required voices covered with good balance |

## Coverage snapshots

`POST /ensembles/{id}/coverage/compute` precomputes coverage for all sheets and stores the results as **snapshots**. Snapshots are displayed as colour-coded badges in the sheets list without re-evaluating on each page load. Snapshots must be manually recomputed — there is no automatic invalidation on sheet changes.

## Ensemble members

Each ensemble has a **membership roster** — the list of musicians who play in it.

`GET /ensembles/{id}/members` · `POST /ensembles/{id}/members` · `PUT /ensembles/{id}/members/{memberId}` · `DELETE /ensembles/{id}/members/{memberId}`

| Field | Type | Notes |
|-------|------|-------|
| Musician | Musician reference | Required |
| Voice | EnsembleVoice reference | Optional — which voice/part the musician fills |
| Instrument | Instrument reference | Optional — instrument played in this ensemble |
| Conductor | Boolean | Marks the ensemble conductor |

A musician may appear multiple times in the same ensemble (once per voice, for players who double on multiple parts). The `voice_id IS NULL` case is unique per musician per ensemble (prevents duplicate conductor entries). Each member can be linked to a Musician entity that has a `userId` (OIDC subject). A musician may belong to the roster without a login (`userId = null`). For authenticated musicians, ensemble membership is the foundation for the [My Parts](my-parts.md) view.

When the musician search filter contains text that matches no existing musician, the Add Member dialog shows a **"Create musician '…'"** button. Clicking it calls `POST /musicians` with the typed name, appends the new musician to the local list, auto-selects them, and shows a success toast — no page navigation required.

## Related

- [Search & Discovery](search-discovery.md) — filtering the sheet list by coverage status
- [Roadmap](../roadmap.md#6-operational--integration) — coverage breakdown enhancements, automatic snapshot invalidation
