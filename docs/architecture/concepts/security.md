# Authentication & Authorization

Quarkus OIDC with self-hosted **Keycloak 26** (`docker-compose.keycloak.yml`; realm export at `keycloak/sam-realm.json`). See [ADR-0003](../decisions/adr-0003-self-hosted-keycloak.md).

| Concern | Mechanism |
|---|---|
| Identity | `Musician.userId` = OIDC `sub` claim. No separate User entity — a musician either has a login or doesn't. See [ADR-0001](../decisions/adr-0001-musician-user-linking.md). |
| Ensemble access | Keycloak group `ensemble:{UUID}` in the JWT `groups` claim, parsed by `CurrentUserService.getAccessibleEnsembleIds()` / `canAccessEnsemble()`. **Not yet enforced:** no endpoint calls these yet, and `SheetCollection.visibility` is stored but not checked — reads are open to every authenticated user. See the [role-aware access plan](../../plans/rbac-role-aware-access.md). |
| Roles | Keycloak realm roles: `music_librarian` (full archive write access), `admin` (system config) |
| Public access | `/api/public/shares/{token}/…` endpoints are `@PermitAll` (no `@Authenticated`); the token is validated manually in `PublicShareResourceImpl` / `ShareService` |
| Ops/diagnostic access | `/q/info`, `/q/metrics` are unauthenticated by design, isolated onto a separate management port (`:9000`) rather than `@Authenticated`'s `/api/*` surface. See [ADR-0007](../decisions/adr-0007-management-interface.md) |
| Machine access | The `cli` module authenticates as the `sam-cli` Keycloak client (confidential client with a service account holding `music_librarian`) via OIDC client-credentials — no user login involved. See `cli/README.md`, "Server URL and authentication". |

`CurrentUserService` (`@RequestScoped`) is the single injection point for identity in business logic — wraps JWT parsing, role checks, and ensemble-group resolution. Inject this instead of `JsonWebToken` directly.

`MyPartsService` extends this pattern for the personalized sheet view: it calls `currentUserService.getUserId()` to look up the linked `MusicianEntity`, collects all instrument IDs from the musician's `EnsembleMembershipEntity` records, and returns only sheets that contain at least one matching instrumentation. Conductor memberships (null instrument) are skipped automatically.

All `*ResourceImpl` classes carry `@Authenticated` at class level. Write methods additionally carry `@RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})`. API interface definitions in the `api` module remain role-free so they can be used as REST clients in the `cli` module.

## Musician–User linking (admin UI)

Admins can link a `Musician` record to an authenticated account via the musician edit form. The link is the `userId` field (OIDC subject claim) on `MusicianEntity`.

Dedicated endpoints (admin-only):
- `PUT /api/musicians/{id}/user/{userId}` — sets the link
- `DELETE /api/musicians/{id}/user` — clears the link

The general `PUT /api/musicians/{id}` (used by the form save) intentionally ignores `userId` via `@Mapping(target = "userId", ignore = true)` in `MusicianMapper`, so a librarian updating a musician's name can never accidentally clear an existing link.

User lookup for the admin search autocomplete is backed by the **Keycloak Admin REST API** via `quarkus-keycloak-admin-rest-client`. The `AdminUsersResource` (`GET /api/admin/users?search=`, `GET /api/admin/users/{id}`) proxies user searches to Keycloak and is restricted to the `admin` role. The admin client authenticates with client credentials as the confidential `sam-backend` client in the `sam` realm (dev secret committed in `application.properties`). In production, configure `KEYCLOAK_ADMIN_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_BACKEND_CLIENT_ID` (default `sam-backend`), and `KEYCLOAK_BACKEND_CLIENT_SECRET`.

## Testing

Test profile: `%test.quarkus.oidc.enabled=false`. Auth-specific tests use `@TestSecurity` from `quarkus-test-security`.

## Related

- [Stakeholders §6](../../stakeholders.md#6-access-control-model) — role matrices (today vs target) and open access-control questions
- [Shares & Public Access feature](../../features/shares.md) — the unauthenticated share-token path
- [Deployment](../deployment.md) — OIDC config endpoint and env vars
