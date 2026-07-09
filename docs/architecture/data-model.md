# Data Model

## Core Entities

```
sheets ──────────< instrumentations >────────── instruments
  |                     |
  |                     +---< attachments >--- documents
  +---< attachments >--- documents
  |
  +---< collection_sheets >--- sheet_collections
  |                             (FOLDER | SETLIST)
  +---< collection_sheets >--- booklets
```

## Ensemble & Coverage Entities

```
ensembles ──< ensemble_voices ──< voice_options >── instruments
                                   (PRIMARY |
                                    ALTERNATE |
                                    FALLBACK)
```

## Entity Details

| Entity | PK | Key Fields |
|--------|----|------------|
| `sheets` | UUID | title, subtitle, composer, arranger, genre, fingerprint |
| `instrumentations` | UUID | sheet (FK), instrument (FK), partLabel, clef, notationType |
| `instruments` | String | name, displayName, transposition, **family**, **defaultClef**, **catalogSection**, **catalogPosition** |
| `instrument_aliases` | (instrument_id, alias_order) | alias strings for OCR matching; ordered list per instrument |
| `musicians` | UUID | name, birthYear, deathYear, ipi, **userId** (OIDC subject — null for external/historical musicians), **email**, **mobile**, **notes**, **status**, **role**, **lastInviteSentAt** |
| `musician_instruments` | UUID | musician (FK), instrument (FK), isPrimary — instrument assignments driving "My Parts" personalisation |
| `documents` | UUID | filename, path, sha256, mimeType, size, refCount |
| `attachments` | UUID | document (FK), type, displayName |
| `sheet_collections` | UUID | name, description, type (FOLDER/SETLIST), date, **visibility**, **coverColor**, **coverImageId** (FK → documents) |
| `booklets` | UUID | name, description |
| `ensembles` | UUID | name, description |
| `ensemble_voices` | UUID | ensemble (FK), label, weight, required |
| `voice_options` | UUID | voice (FK), instrument (FK), type, factor |
| `ensemble_memberships` | UUID | musician (FK), ensemble (FK), voice (FK, nullable), instrument (FK, nullable), conductor (bool) — one row per musician per voice per ensemble |
| `shares` | UUID | creatorUserId, resourceType, resourceId, expiresAt, revokedAt, createdAt |
| `event_log` | UUID | occurredAt, userId, username, eventType, entityType, entityId, metadata (JSONB), shareTokenId |

All domain entities carry `version` (optimistic locking), `created`, `lastUpdate` timestamps, and have `_AUD` audit mirror tables via Hibernate Envers. `shares` and `event_log` are append-only and are not audited.

## Related

- [Search](concepts/search.md) — the tsvector/trigram/phonetic search columns on `sheets`
- [Audit & Event Log](concepts/audit.md) — `_AUD` tables and the `event_log`
- [Coverage Evaluation](concepts/coverage.md) — how ensemble/voice entities are used
- Flyway migrations: `server/src/main/resources/db/migration/`
