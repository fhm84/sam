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
| `SHARE_CREATED` | Share token created by an authenticated user |
| `SHARE_ACCESSED` | Shared resource accessed via a share token |
| `SHARE_REVOKED` | Share token revoked by its creator |
| `SETLIST_AI_SUGGESTION_GENERATED` | AI setlist assistant generated program suggestions |
| `SETLIST_AI_TEXT_DRAFTED` | AI assistant drafted programme text for a TEXT item |

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

`/admin/event-logs` — read-only page with filtering by event type (multi-select), user ID, and entity type. `GET /api/event-logs` is `admin` only.

**My activity** (`/user/my-activity`, user menu next to My data) shows every user their own entries: `GET /api/me/event-logs` (paginated, newest first) returns only the entries whose `userId` is the caller's token subject, never share-link entries or other users' (GDPR Art. 15). A login without a subject gets an empty page, not the whole log.

## Retention

A nightly job (`EventLogRetention`, 03:15) deletes entries older than `sam.event-log.retention-months` (default 12, env var `SAM_EVENT_LOG_RETENTION_MONTHS`; `0` turns it off, a negative value stops the server at startup), share-link entries included, in one `DELETE`. The time is `sam.event-log.retention-cron` (`SAM_EVENT_LOG_RETENTION_CRON`). Entries of a member who leaves or is erased are anonymised first and then age out the same way.

## Related

- [Shares & Public Access](shares.md) — share-link access logging
- [Members](members.md#retention) — entries of a member who leaves or is erased are anonymised (user ID `erased`)
- [Production setup](../architecture/production-setup.md#privacy-policy-note) — what to put into the ensemble's privacy policy
- [Roadmap](../roadmap.md#document-access-log--partial) — richer UI still pending
