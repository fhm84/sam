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
| Musicians | `/api/musicians` | |
| Instruments | `/api/instruments` | |
| Sheet collections | `/api/sheet-collections` | Including `/items` sub-resource, `/{id}/toc` (PDF), `/{id}/gema-setlist` (xlsx), `/{id}/export`, and `/{id}/ai/suggest-items` (AI setlist assistant) |
| Collection items | `/api/sheet-collections/{id}/items` | Sub-resource; including `/{itemId}/ai/draft-text` (AI programme-text drafting) |
| Ensembles | `/api/ensembles` | Including `/coverage/compute`, `/coverage/status` |
| Ensemble voices | `/api/ensembles/{id}/voices` | Sub-resource |
| Voice options | `/api/ensembles/{id}/voices/{vid}/options` | Sub-resource |
| Ensemble members | `/api/ensembles/{id}/members` | Sub-resource |
| Shares | `/api/shares` | Authenticated share management (create, list, revoke) |
| Public share | `/api/public/shares/{token}` | Unauthenticated; token-validated (`/download`, `/toc`, `/download/{instrumentationId}`, `/sheet/{attachmentId}`) — see [Shares](shares.md) |
| Admin users | `/api/admin/users` | `admin` only; user search for musician–account linking through the OIDC provider (Pocket ID or Keycloak); `/directory` tells whether search is available (501 without a Pocket ID API key) |
| Event log | `/api/event-logs` | Read-only; requires authentication |
| My parts | `/api/me/parts` | Authenticated; paginated sheets for the calling user's instruments |
| App info | `/q/info` | Unauthenticated; management port (`:9000`), not `/api`. Git branch/commit, build timestamp, Quarkus/Java/OS versions |
| Metrics | `/q/metrics` | Unauthenticated; management port (`:9000`), not `/api`. Prometheus scrape endpoint |

## Access control

All `/api/*` endpoints require authentication (valid OIDC bearer token), except the public share
endpoints under `/api/public/shares/` (`@PermitAll`, token validated manually in
`PublicShareResourceImpl`). Write operations (POST, PUT, DELETE) on archive data additionally
require the `music_librarian` or `admin` realm role; exceptions are share management
(`/api/shares`, scoped to the caller's own shares — see [Shares](shares.md) for who may create
links) and marking a sheet as favourite. Read operations (GET) are open to any authenticated user unless the table below says
otherwise; ensemble scoping and collection visibility are not enforced yet (see the
[role-aware access plan](../plans/rbac-role-aware-access.md)). Role enforcement uses
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
| `/musicians`, `/musicians/{id}` | any | any; `contact` (email, mobile, notes), `membership.lastInviteSentAt` and `userId` only for librarian + admin | [#132](https://github.com/fhm84/sam/issues/132) |
| `/ensembles/{id}/members` | any | any; the nested `musician` stripped as above | [#132](https://github.com/fhm84/sam/issues/132) |
| `/instruments`, `/instruments/{id}` | any | any | |
| `/ensembles`, `/ensembles/{id}`, `/voices`, `/voices/{vid}/options`, `/coverage/status` | any | any | |
| `/sheet-collections`, `/{id}`, `/{id}/items`, `/{id}/toc`, `/{id}/export` | any | any, filtered by `visibility`: `WHOLE_ENSEMBLE` any, `ADMINS_ONLY` and `PRIVATE` librarian + admin (collections store no creator, so `PRIVATE` can't mean "creator only" yet; none exists today) | [#243](https://github.com/fhm84/sam/issues/243) |
| `/sheet-collections/{id}/gema-setlist` (GEMA report) | librarian + admin | librarian + admin | done |
| `/shares` | own shares | own shares | |
| `/me/parts` | own | own | |
| `/event-logs` | any | admin | [#131](https://github.com/fhm84/sam/issues/131); own history in [#146](https://github.com/fhm84/sam/issues/146) |
| `/admin/users` | admin | admin | |
| `/oidc-config.json`, `/public/shares/{token}/…` | public | public | |

UI routes, for the route guard ([#133](https://github.com/fhm84/sam/issues/133)) and menu ([#134](https://github.com/fhm84/sam/issues/134)):

| Route | Who |
|---|---|
| `sheets`, `collections`, `my-parts`, `shares`, `musicians`, `user/preferences` | any |
| `uploads`, `admin/ensembles`, `admin/instruments` | librarian + admin |
| `admin/configuration`, `admin/event-logs` | admin |

Also unauthenticated, outside `/api/*`:
- `/q/*` (management port `:9000`) — `/q/info` and `/q/metrics`; see [Security concept](../architecture/concepts/security.md) and [ADR-0007](../architecture/decisions/adr-0007-management-interface.md) for the rationale.

## Related

- [API Design concept](../architecture/concepts/api-design.md) — sub-resource pattern, shared interfaces
- [Security concept](../architecture/concepts/security.md) — roles and enforcement details
