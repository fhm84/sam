# Authentication & Authorization

Quarkus OIDC with self-hosted **Keycloak 26** (`docker-compose.keycloak.yml`; realm export at `keycloak/sam-realm.json`). See [ADR-0003](../decisions/adr-0003-self-hosted-keycloak.md).

| Concern | Mechanism |
|---|---|
| Identity | `Musician.userId` = OIDC `sub` claim. No separate User entity — a musician either has a login or doesn't. See [ADR-0001](../decisions/adr-0001-musician-user-linking.md). |
| Ensemble access | Keycloak group `ensemble:{UUID}` in the JWT `groups` claim, parsed by `CurrentUserService.getAccessibleEnsembleIds()` / `canAccessEnsemble()`. **Not yet enforced:** no endpoint calls these yet, and `SheetCollection.visibility` is stored but not checked — reads are open to every authenticated user. See the [role-aware access plan](../../plans/rbac-role-aware-access.md). |
| Roles | `music_librarian` (full archive write access), `admin` (system config). Keycloak: realm roles. Pocket ID: only the SAM-specific groups `sam-music-librarian` / `sam-admin` grant them (`sam.oidc.role-groups`, `RoleGroupsAugmentor`), so a shared Pocket ID's other groups grant nothing in SAM |
| Token audience | Only tokens meant for SAM are accepted: Pocket ID `aud` must contain the UI client ID (`sam-ui`), Keycloak `aud` must contain `urn:sam:api` (audience mapper on `sam-ui` and `sam-cli`); the `cli`'s Pocket ID token goes to the `m2m` tenant, which requires `urn:sam:api` |
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

User lookup for the admin search autocomplete goes through `UserDirectory`, one implementation per provider, picked by `sam.oidc.provider`. `AdminUsersResource` (`GET /api/admin/users?search=`, `GET /api/admin/users/{id}`, `GET /api/admin/users/directory`) is restricted to the `admin` role; user IDs are the provider's `sub`.

- **Pocket ID** (`PocketIdUserDirectory`): calls Pocket ID's `GET /api/users` with an API key (`X-API-KEY`), set as `SAM_OIDC_POCKET_ID_API_KEY` (optional `SAM_OIDC_POCKET_ID_URL` if SAM reaches Pocket ID under another address than the issuer URL). Pocket ID API keys cannot be scoped: the key acts with full admin rights on Pocket ID and every app behind it. So it lives only in the server's env, is never logged (errors name the HTTP status, never the key), and is **optional**: without it, `GET /directory` answers `searchable: false`, search answers 501, and the musician form asks for the user's ID instead (an admin copies it from the user's page in Pocket ID). Dev uses the static key of `docker-compose.pocketid.yml`.
  - **Rotating the key:** in Pocket ID, *Settings → Administration → API Keys* (`/settings/admin/api-keys`), create a new key (with an expiry), set it as `SAM_OIDC_POCKET_ID_API_KEY`, restart `sam-server`, check that the musician form's user search works, then revoke the old key. Rotate at least when it expires and whenever the server or its `.env` may have been exposed.
- **Keycloak** (`KeycloakUserDirectory`): the Keycloak Admin REST API via `quarkus-keycloak-admin-rest-client`, authenticated with client credentials as the confidential `sam-backend` client (role `view-users` only; dev secret in `oidc/keycloak.properties`). In production, configure `KEYCLOAK_ADMIN_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_BACKEND_CLIENT_ID` (default `sam-backend`), and `KEYCLOAK_BACKEND_CLIENT_SECRET`.

A failing provider API (unreachable, rejected key) answers 502.

## Testing

Test profile: `%test.quarkus.oidc.enabled=false`. Auth-specific tests use `@TestSecurity` from `quarkus-test-security`.

## Related

- [Stakeholders §6](../../stakeholders.md#6-access-control-model) — role matrices (today vs target) and open access-control questions
- [Shares & Public Access feature](../../features/shares.md) — the unauthenticated share-token path
- [Deployment](../deployment.md) — OIDC config endpoint and env vars
