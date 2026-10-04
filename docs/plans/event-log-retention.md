# Plan: Event Log Retention & Self-Service Access

Roadmap: [Document access log](../roadmap.md#document-access-log--partial)
· Feature: [Event log](../features/event-log.md)
· Decision: [ADR-0004 no IP logging](../architecture/decisions/adr-0004-no-ip-logging.md)

**Why now:** once real users log in, `event_log` holds personal data (who downloaded
what, when). GDPR then expects a defined retention period and a way for users to see
their own entries before going live, not after.

## Decision needed: open question #9

Proposed answer: **12 months**, configurable via `sam.event-log.retention-months`
(default 12; 0 disables deletion). Share-link entries (no user) follow the same rule.

## Tasks

| # | Task | Status |
|---|------|--------|
| 1 | Add the `quarkus-scheduler` extension and a nightly job that deletes `event_log` rows older than the configured period (single bulk `DELETE`, logged count). | pending |
| 2 | Config property with validation, documented in `docs/architecture/deployment.md` (env var). | pending |
| 3 | `GET /api/me/event-logs`: the caller's own entries (paginated, filtered by `userId` = token subject). This covers the GDPR right of access. | pending |
| 4 | "My activity" page under user preferences, reusing the event-log table component. | pending |
| 5 | Tests for the retention job (seed old and new rows, run the job, assert only the old rows are gone) and for `me/event-logs` scoping. | pending |
| 6 | Close open question #9, update `docs/features/event-log.md`, and add a privacy-policy note template to the production setup guide. | pending |

Admin-only access to the full log is handled in [role-aware access](rbac-role-aware-access.md), task 2.

## Key files

- `server/src/main/java/de/halbmann/sam/business/eventlog/` (`EventLogService`, `EventLogRepository`, `EventLogEntity`)
- `api/src/main/java/de/halbmann/sam/api/boundary/EventLogResource.java`
- `ui/src/main/webui/src/app/features/event-logs/`, `features/user-preferences/`
