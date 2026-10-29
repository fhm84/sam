# Data Model

The diagrams show tables and their foreign keys as created by the Flyway migrations; the
columns are listed in [Entity Details](#entity-details). Join tables (`sheets_attachments`,
`instrumentations_attachments`, `sheet_collections_items`) are drawn as the relationship
they implement, and `_AUD` tables are left out.

## Core Entities

```mermaid
erDiagram
    persons |o--o{ sheets : "composer, arranger"
    persons ||--o| members : "member profile (same id)"
    sheets ||--o{ instrumentations : "parts"
    instruments ||--o{ instrumentations : "instrument"
    sheets |o--o{ attachments : "sheet files"
    instrumentations |o--o{ attachments : "part files"
    documents ||--o{ attachments : "stored file"
    attachments ||--o| classifications : "AI classification"
    sheet_collections ||--o{ collection_items : "ordered items"
    sheets |o--o{ collection_items : "SHEET item"
    attachments |o--o{ collection_items : "TEXT item file"
    documents |o--o{ sheet_collections : "cover image"
```

Collections are `FOLDER`s or `SETLIST`s; their items are `SHEET` or `TEXT` rows.

## Ensemble & Coverage Entities

```mermaid
erDiagram
    ensembles ||--o{ ensemble_voices : "voices"
    ensemble_voices ||--o{ voice_options : "options"
    instruments ||--o{ voice_options : "instrument"
    ensembles ||--o{ ensemble_memberships : "members"
    members ||--o{ ensemble_memberships : "seat"
    ensemble_voices |o--o{ ensemble_memberships : "voice played"
    instruments |o--o{ ensemble_memberships : "instrument played"
    members ||--o{ member_instruments : "plays"
    instruments ||--o{ member_instruments : "instrument"
    ensembles ||--o{ coverage_snapshots : "evaluated for"
    sheets ||--o{ coverage_snapshots : "cached coverage"
    ensembles |o--o{ sheet_collections : "AI suggestion scope"
```

Voice options are `PRIMARY`, `ALTERNATE`, or `FALLBACK`. `shares` and `event_log` have no
foreign keys: they reference their resource by type and ID, so they also cover deleted
entities.

## Entity Details

| Entity | PK | Key Fields |
|--------|----|------------|
| `sheets` | UUID | title, subtitle, composer, arranger, genre, fingerprint, **source**, **rightsStatus**, **gemaReportable**, **tempo**, **tonality**, **arrangementPublisher**, **arrangementRightsUntil** |
| `instrumentations` | UUID | sheet (FK), instrument (FK), partLabel, clef, notationType, **pages**, physicalLocation, physicalCondition |
| `instruments` | String | name, displayName, transposition, **family**, **defaultClef**, **catalogSection**, **catalogPosition** |
| `instrument_aliases` | (instrument_id, alias_order) | alias strings for OCR matching; ordered list per instrument |
| `persons` | UUID | name, birthYear, deathYear, ipi — everyone credited on sheets, band members included ([ADR-0015](decisions/adr-0015-person-catalogue-and-member-profile.md)) |
| `members` | UUID (= the person's id, FK → persons) | **email**, **mobile**, **notes**, **membershipStatus** (ACTIVE/PAUSED/FORMER), **leftAt**, **lastInviteSentAt**, **userId** (OIDC subject, unique) — the personal data of a band member |
| `member_instruments` | UUID | member (FK), instrument (FK), isPrimary — instruments a member can play |
| `documents` | UUID | filename, path, sha256, mimeType, size, refCount |
| `attachments` | UUID | document (FK), type, displayName |
| `sheet_collections` | UUID | name, description, type (FOLDER/SETLIST), date, **visibility**, **coverColor**, **coverImageId** (FK → documents), **ensembleId** (FK → ensembles, nullable, ON DELETE SET NULL — scopes coverage-aware AI suggestions), **createdBy** (OIDC `sub` of the creator, nullable for older collections — makes `PRIVATE` a creator's draft) |
| `collection_items` | UUID | item_type discriminator (SHEET/TEXT, single-table inheritance): SHEET rows carry sheet (FK), TEXT rows carry identifier, textContent, attachment (FK, nullable); ordered per collection via `sheet_collections_items` join table |
| `ensembles` | UUID | name, description |
| `ensemble_voices` | UUID | ensemble (FK), label, weight, required |
| `voice_options` | UUID | voice (FK), instrument (FK), type, factor |
| `ensemble_memberships` | UUID | member (FK), ensemble (FK), voice (FK, nullable), instrument (FK, nullable), **role** (MEMBER/GUEST/SUBSTITUTE/CONDUCTOR) — one row (seat) per member per voice per ensemble; a member's voice seats in one ensemble share the role, driving "My Parts" personalisation |
| `shares` | UUID | creatorUserId, resourceType, resourceId, expiresAt, revokedAt, createdAt |
| `event_log` | UUID | occurredAt, userId, username, eventType, entityType, entityId, metadata (JSONB), shareTokenId |
| `classifications` | UUID | attachment (FK, unique), status — AI classification request for an uploaded document not yet linked to a sheet |
| `coverage_snapshots` | UUID | ensemble (FK), sheet (FK), score, status (COMPLETE/PLAYABLE/INCOMPLETE), missingRequired, details (JSONB) — one row per ensemble and sheet |

All domain entities carry `version` (optimistic locking), `created`, `lastUpdate` timestamps, and have `_AUD` audit mirror tables via Hibernate Envers. `shares` and `event_log` are append-only and are not audited; `coverage_snapshots` is computed data and not audited either.

## Related

- [Search](concepts/search.md) — the tsvector/trigram/phonetic search columns on `sheets`
- [Audit & Event Log](concepts/audit.md) — `_AUD` tables and the `event_log`
- [Coverage Evaluation](concepts/coverage.md) — how ensemble/voice entities are used
- Flyway migrations: `server/src/main/resources/db/migration/`
- Flyway is the only source of the schema. Hibernate validates the mapping against it in prod (`validate`) and in `FlywaySchemaValidationTest`; its `update` mode is enabled for the local `%dev` profile only.
