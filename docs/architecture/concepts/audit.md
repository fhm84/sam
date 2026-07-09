# Audit Trail & Event Log

## Envers audit trail (writes)

Every domain entity is annotated with `@Audited` (Hibernate Envers). Each table has a corresponding `_AUD` table using `ValidityAuditStrategy` (tracks both revision start and end). This provides a complete history of all data mutations.

## Event log (reads)

Read events (document downloads, exports, AI classification, share-link access) are tracked separately in the `event_log` table via `EventLogService` — Envers only covers writes.

Share-link access is logged with `shareTokenId` set and `userId`/`username` as `null`. IP addresses are deliberately not stored (see [ADR-0004](../decisions/adr-0004-no-ip-logging.md)).

## Related

- [Event Log feature](../../features/event-log.md) — recorded event types and the admin UI
- [Data Model](../data-model.md) — `event_log` table fields
