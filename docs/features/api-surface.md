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
| Admin users | `/api/admin/users` | `admin` only; Keycloak user search for musician–account linking |
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
links) and marking a sheet as favourite. Read operations (GET) are currently accessible to any authenticated user — ensemble
scoping and collection visibility are not enforced yet (see the
[role-aware access plan](../plans/rbac-role-aware-access.md)). Role enforcement uses
`@RolesAllowed` on the JAX-RS implementation classes; the API interface definitions remain
role-free to stay usable as a REST client in the CLI module.

Also unauthenticated, outside `/api/*`:
- `/q/*` (management port `:9000`) — `/q/info` and `/q/metrics`; see [Security concept](../architecture/concepts/security.md) and [ADR-0007](../architecture/decisions/adr-0007-management-interface.md) for the rationale.

## Related

- [API Design concept](../architecture/concepts/api-design.md) — sub-resource pattern, shared interfaces
- [Security concept](../architecture/concepts/security.md) — roles and enforcement details
