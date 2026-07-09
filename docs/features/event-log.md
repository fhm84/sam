# Event Log

A write-once access and activity log. Captures read events (downloads, exports) that Hibernate Envers does not track — see [Audit & Event Log concept](../architecture/concepts/audit.md).

## Recorded events

| Event type | Trigger |
|------------|---------|
| `DOCUMENT_DOWNLOAD` | Single document served |
| `DOCUMENT_BATCH_DOWNLOAD` | ZIP or merged-PDF batch download |
| `SHEET_EXPORT` | Sheet exported as JSON, CSV, or ZIP |
| `COLLECTION_EXPORT` | Collection exported |
| `COLLECTION_TOC_GENERATED` | Collection table of contents PDF generated |
| `GEMA_SETLIST_GENERATED` | GEMA setlist xlsx generated |
| `DOCUMENT_CLASSIFIED` | AI classification run on a document |
| `DOCUMENT_CLASSIFICATION_APPLIED` | AI classification result applied |

## Log entry fields

| Field | Notes |
|-------|-------|
| `occurredAt` | Timestamp with timezone |
| `userId` | OIDC subject (null for share-link access) |
| `username` | Snapshotted `preferred_username` (null for share-link access) |
| `eventType` | One of the types above |
| `entityType` / `entityId` | The target entity |
| `metadata` | JSONB payload (filename, count, format, etc.) |
| `shareTokenId` | Set when the event was triggered via a share link; `userId`/`username` are null in that case |

IP addresses are not stored ([ADR-0004](../architecture/decisions/adr-0004-no-ip-logging.md)). `userId` + `username` give unambiguous attribution for authenticated access; share-link access is identified by `shareTokenId`.

## Event log UI

`/admin/event-logs` — read-only page with filtering by event type (multi-select), user ID, and entity type.

## Related

- [Shares & Public Access](shares.md) — share-link access logging
- [Roadmap](../roadmap.md#6-operational--integration) — retention policy and richer UI still pending
