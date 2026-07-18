# API Surface Summary

All endpoints are under the `/api` base path.

| Resource | Base path | Notes |
|----------|-----------|-------|
| Sheets | `/api/sheets` | Includes `/enrich`, `/coverage`, `/explore` (+ `/explore/surprise`) |
| Instrumentations | `/api/sheets/{id}/instrumentations` | Sub-resource |
| Sheet documents | `/api/sheets/{id}/documents` | Sub-resource |
| Instrumentation documents | `/api/sheets/{sid}/instrumentations/{iid}/documents` | Sub-resource |
| Global documents | `/api/documents` | Unlinked pool, classify, apply |
| Musicians | `/api/musicians` | |
| Instruments | `/api/instruments` | |
| Sheet collections | `/api/sheet-collections` | Including `/sheets` sub-resource |
| Booklets | `/api/booklets` | Including `/sheets` sub-resource |
| Ensembles | `/api/ensembles` | Including `/coverage/compute`, `/coverage/status` |
| Ensemble voices | `/api/ensembles/{id}/voices` | Sub-resource |
| Voice options | `/api/ensembles/{id}/voices/{vid}/options` | Sub-resource |
| Ensemble members | `/api/ensembles/{id}/members` | Sub-resource |
| Shares | `/api/shares` | Authenticated share management (create, list, revoke) |
| Public share | `/public/share/{token}` | Unauthenticated; token-validated resource access |
| Event log | `/api/event-logs` | Read-only; requires authentication |
| My parts | `/api/me/parts` | Authenticated; paginated sheets for the calling user's instruments |

## Access control

All `/api/*` endpoints require authentication (valid OIDC bearer token). Write operations (POST, PUT, DELETE) additionally require the `music_librarian` or `admin` realm role. Read operations (GET) are accessible to any authenticated user. The `/public/share/{token}` endpoint is explicitly unauthenticated — it validates the share token manually in a separate resource class with no `@Authenticated` class-level annotation. Role enforcement uses `@RolesAllowed` on the JAX-RS implementation classes; the API interface definitions remain role-free to stay usable as a REST client in the CLI module.

## Related

- [API Design concept](../architecture/concepts/api-design.md) — sub-resource pattern, shared interfaces
- [Security concept](../architecture/concepts/security.md) — roles and enforcement details
