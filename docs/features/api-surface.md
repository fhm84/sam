# API Surface Summary

Business endpoints are under the `/api` base path. Ops/diagnostic endpoints
(`/q/*`) live on a separate, unauthenticated management port — see
[Monitoring](../../monitoring/CLAUDE.md) and
[ADR-0007](../architecture/decisions/adr-0007-management-interface.md).

| Resource | Base path | Notes |
|----------|-----------|-------|
| Sheets | `/api/sheets` | Includes `/genres`, `/letters`, `/explore` (+ `/explore/surprise`); per sheet `/enrich`, `/coverage`, `/export`, `/tags`, `/favorite`, `/collections` |
| Instrumentations | `/api/sheets/{id}/instrumentations` | Sub-resource; `POST /bulk` creates several at once |
| Sheet documents | `/api/sheets/{id}/documents` | Sub-resource |
| Instrumentation documents | `/api/sheets/{sid}/instrumentations/{iid}/documents` | Sub-resource |
| Global documents | `/api/documents` | Unlinked pool (`/unlinked`), `/batch` download by attachment IDs (ZIP or merged PDF), `/{id}/link`, `/{id}/classify`, `/{id}/apply` |
| People | `/api/persons` | Name, years, IPI; writes for `music_librarian` / `admin`; a credited person or a member can't be deleted (409); `/{id}/credits` lists the sheets crediting the person; `POST /{id}/merge-into/{targetId}` merges a duplicate (librarian / admin) |
| Members | `/api/members` | Contact details, linked login, last invite time and onboarding state only for `music_librarian` / `admin`; `DELETE` erases the profile, seats and their audit history |
| Instruments | `/api/instruments` | |
| Sheet collections | `/api/sheet-collections` | Including `/items` sub-resource, `/{id}/toc` (PDF), `/{id}/gema-setlist` (xlsx), `/{id}/export`, and `/{id}/ai/suggest-items` (AI setlist assistant) |
| Collection items | `/api/sheet-collections/{id}/items` | Sub-resource; including `/{itemId}/ai/draft-text` (AI programme-text drafting) |
| Ensembles | `/api/ensembles` | Including `/coverage/compute`, `/coverage/status` |
| Ensemble voices | `/api/ensembles/{id}/voices` | Sub-resource |
| Voice options | `/api/ensembles/{id}/voices/{vid}/options` | Sub-resource |
| Ensemble members | `/api/ensembles/{id}/members` | Sub-resource |
| Shares | `/api/shares` | Authenticated share management (create, list, revoke) |
| Public share | `/api/public/shares/{token}` | Unauthenticated; token-validated (`/download`, `/toc`, `/download/{instrumentationId}`, `/sheet/{attachmentId}`) — see [Shares](shares.md) |
| Admin storage | `/api/admin/storage/integrity-check` | `admin` only; `POST` re-hashes every stored file and reports damaged, missing, orphaned and leftover temp files (read-only) |
| Admin users | `/api/admin/users` | `admin` only; user search for member–account linking through the OIDC provider (Pocket ID or Keycloak); `/directory` tells whether search is available (501 without a Pocket ID API key) |
| Event log | `/api/event-logs` | Read-only; `admin` only |
| My parts | `/api/me/parts` | Authenticated; paginated sheets for the calling user's instruments |
| My activity | `/api/me/event-logs` | Authenticated; paginated, the caller's own event-log entries (never share-link or other users' entries) |
| My member data | `/api/me/member` | Authenticated; the caller's own member profile with personal data and seats (404 without a linked member) |
| App info | `/q/info` | Unauthenticated; management port (`:9000`), not `/api`. Git branch/commit, build timestamp, Quarkus/Java/OS versions |
| Metrics | `/q/metrics` | Unauthenticated; management port (`:9000`), not `/api`. Prometheus scrape endpoint |
| Health | `/q/health`, `/q/health/ready`, `/q/health/live` | Unauthenticated; management port (`:9000`), not `/api`. Readiness: database and storage; used by the prod compose healthcheck ([ADR-0013](../architecture/decisions/adr-0013-readiness-gates-startup.md)) |

## Access control

All `/api/*` endpoints require authentication (valid OIDC bearer token), except the public share
endpoints under `/api/public/shares/` (`@PermitAll`, token validated manually in
`PublicShareResourceImpl`). Write operations (POST, PUT, DELETE) on archive data additionally
require the `music_librarian` or `admin` role; exceptions are share management
(`/api/shares`, scoped to the caller's own shares — see [Shares](shares.md) for who may create
links) and marking a sheet as favourite. Read operations (GET) are open to any authenticated user unless the table below says
otherwise; nothing scopes reads per ensemble yet (see the
[roadmap](../roadmap.md#full-role-based-access-control-rbac--in-progress)). Role enforcement uses
`@RolesAllowed` on the JAX-RS implementation classes; the API interface definitions remain
role-free to stay usable as a REST client in the CLI module.

### Read access by role

**Decided in [#130](https://github.com/fhm84/sam/issues/130) (2026-10-24); the Follow-up column names the issue that enforces a row.**
The rule of thumb: every login reads the music (sheets, parts, collections, ensembles, its
own data); the music librarian also sees the working areas and other people's personal
data; only the admin sees accounts and the event log. "Any" means any authenticated user.
Endpoints not listed (writes) keep their `@RolesAllowed` as today.

| Endpoint (GET unless noted) | Today | Decided | Follow-up |
|---|---|---|---|
| `/sheets`, `/sheets/{id}`, `/genres`, `/letters`, `/explore`, `/explore/surprise`, `/{id}/coverage`, `/{id}/collections`, `/{id}/export` | any | any | |
| `/sheets/{id}/instrumentations`, `…/{iid}` | any | any | |
| Sheet and instrumentation `…/documents`, `/documents/{id}`, `/documents/batch` (GET and POST by IDs) | any | any — musicians need their parts | |
| `/documents` without sheet context (always empty) | any | any | |
| `/documents/unlinked` (upload inbox, not yet reviewed) | librarian + admin | librarian + admin | done |
| `/members`, `/members/{id}` | any | any; `contact` (email, mobile, notes), `lastInviteSentAt`, `onboarding` and `userId` only for librarian + admin (sheets' `composer` / `arranger` are persons and carry no personal data) | done |
| `/ensembles/{id}/members` | any | any; the nested `member` stripped as above | done |
| `/instruments`, `/instruments/{id}` | any | any | |
| `/ensembles`, `/ensembles/{id}`, `/voices`, `/voices/{vid}/options`, `/coverage/status` | any | any | |
| `/sheet-collections`, `/{id}`, `/{id}/items`, `/{id}/toc`, `/{id}/export` | any | any, filtered by `visibility`: `WHOLE_ENSEMBLE` any, `ADMINS_ONLY` librarian + admin, `PRIVATE` creator + admin (+ librarians if no creator is recorded); a hidden one answers 404, also to writes; also filters a sheet's `/collections` | done |
| `/sheet-collections/{id}/gema-setlist` (GEMA report) | librarian + admin | librarian + admin | done |
| `/shares` | own shares | own shares | |
| `/me/parts` | own | own | |
| `/me/member` | own | own | |
| `/me/event-logs` | own | own | done |
| `/event-logs` | admin | admin | done |
| `/admin/users` | admin | admin | |
| `/admin/storage/integrity-check` | admin | admin | |
| `/oidc-config.json`, `/public/shares/{token}/…` | public | public | |

UI routes, enforced by `roleGuard` and the menu's `roles` (sheet create/edit forms count as librarian + admin):

| Route | Who |
|---|---|
| `sheets`, `collections`, `my-parts`, `shares`, `members`, `persons`, `user/preferences`, `user/my-data`, `user/my-activity` | any |
| `uploads`, `admin/ensembles`, `admin/instruments` | librarian + admin |
| `admin/configuration`, `admin/event-logs` | admin |

Also unauthenticated, outside `/api/*`:
- `/q/*` (management port `:9000`) — `/q/info`, `/q/metrics` and `/q/health`; see [Security concept](../architecture/concepts/security.md) and [ADR-0007](../architecture/decisions/adr-0007-management-interface.md) for the rationale.

## Related

- [API Design concept](../architecture/concepts/api-design.md) — sub-resource pattern, shared interfaces
- [Security concept](../architecture/concepts/security.md) — roles and enforcement details
