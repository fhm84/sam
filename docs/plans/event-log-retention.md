# Plan: Event Log Retention & Self-Service Access

Roadmap: [Document access log](../roadmap.md#document-access-log--partial)
· Tracking issue: [#146](https://github.com/fhm84/sam/issues/146) (milestone [Go-live](https://github.com/fhm84/sam/milestone/1))
· Feature: [Event log](../features/event-log.md)
· Decision: [ADR-0004 no IP logging](../architecture/decisions/adr-0004-no-ip-logging.md)

**Why now:** once real users log in, `event_log` holds personal data (who downloaded
what, when). GDPR then expects a defined retention period and a way for users to see
their own entries before going live, not after.

## Decision needed: open question #9

Discussed in [#147](https://github.com/fhm84/sam/issues/147).

Proposed answer: **12 months**, configurable via `sam.event-log.retention-months`
(default 12; 0 disables deletion). Share-link entries (no user) follow the same rule.

## Tasks

| # | Task | Status |
|---|------|--------|
| 1 | Add the `quarkus-scheduler` extension and a nightly job that deletes `event_log` rows older than the configured period (single bulk `DELETE`, logged count). | [#148](https://github.com/fhm84/sam/issues/148) |
| 2 | Config property with validation, documented in `docs/architecture/deployment.md` (env var). | [#149](https://github.com/fhm84/sam/issues/149) |
| 3 | `GET /api/me/event-logs`: the caller's own entries (paginated, filtered by `userId` = token subject). This covers the GDPR right of access. | [#150](https://github.com/fhm84/sam/issues/150) |
| 4 | "My activity" page under user preferences, reusing the event-log table component. | [#151](https://github.com/fhm84/sam/issues/151) |
| 5 | Tests for the retention job (seed old and new rows, run the job, assert only the old rows are gone) and for `me/event-logs` scoping. | [#152](https://github.com/fhm84/sam/issues/152) |
| 6 | Close open question #9, update `docs/features/event-log.md`, and add a privacy-policy note template to the production setup guide. | [#153](https://github.com/fhm84/sam/issues/153) |

Admin-only access to the full log shipped with role-aware access ([#131](https://github.com/fhm84/sam/issues/131)); see [Event log](../features/event-log.md).

The [member profiles plan](people-catalogue.md#gdpr-dsgvo) reuses the nightly job from
task 1 for former members and old audit rows, and the "My data" page sits next to "My
activity" from task 4. When a member leaves or is erased, their entries here are anonymised
(fixed placeholder `userId`, no `username`) rather than deleted, and age out as usual. The
`quarkus-scheduler` extension is already in place (member retention job), so task 1 only needs
its own `@Scheduled` method.

## Key files

- `server/src/main/java/de/halbmann/sam/business/eventlog/` (`EventLogService`, `EventLogRepository`, `EventLogEntity`)
- `api/src/main/java/de/halbmann/sam/api/boundary/EventLogResource.java`
- `ui/src/main/webui/src/app/features/event-logs/`, `features/user-preferences/`
