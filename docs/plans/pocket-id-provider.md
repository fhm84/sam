# Plan: Pocket ID as Default OIDC Provider

Roadmap: [Pocket ID as default OIDC provider](../roadmap.md#pocket-id-as-default-oidc-provider--planned)
· Tracking issue: [#210](https://github.com/fhm84/sam/issues/210)
· Concept: [Security](../architecture/concepts/security.md)
· Revisits: [ADR-0003 self-hosted Keycloak](../architecture/decisions/adr-0003-self-hosted-keycloak.md)

**Why now:** Keycloak is the heaviest part of a small deployment: about 425–600 MB RAM
idle plus its own Postgres, and a realm export that has to be kept in sync by hand.
[Pocket ID](https://pocket-id.org) does the job for a band with one Go binary (~20 MB
idle) and passkeys instead of passwords, which suits members who log in a few times a
year. It changes the production stack, so it lands before go-live.

## Decisions (2026-10-17)

- **Pocket ID is the default; Keycloak stays supported.** The code stays plain OIDC, and
  one setting selects the provider. Both setups are documented and tested.
- **Musician linking uses the active provider's user API** (Pocket ID `/api/users` with an
  API key, Keycloak's admin client), behind one interface.
- **Dev switches to Pocket ID** as well.
- **Passkeys are acceptable for now.** Recovery is a login code sent by an admin; real
  devices are checked before go-live (task 11).

## Spike results (2026-10-17, Pocket ID v2.17.0, dev)

| Check | Result |
|---|---|
| Idle memory | ~20 MB (Keycloak next to it: ~425 MB) |
| Angular login (code + PKCE, refresh token, silent sign-in in a new tab, deep link kept) | ✅ |
| Passkey register + log in (virtual authenticator), `amr: ["phr"]` | ✅ |
| Login codes (admin creates a one-time link, `/lc/<code>`) | ✅ |
| Roles: librarian writes, musician gets 403, admin reaches admin endpoints and UI | ✅ |
| Asking for `scope=… admin music_librarian` as a user does not grant those scopes | ✅ |
| `cli` client credentials (`list -v` on the real archive; write + delete with the same token) | ✅ |
| Musician ↔ user linking (`/api/admin/users`) | ❌ still the Keycloak admin client (task 3) |

**The provider quirk:** Pocket ID puts `groups` only into the ID token and userinfo, never
into the access token ([pocket-id#1389](https://github.com/pocket-id/pocket-id/issues/1389),
open). Client-credentials tokens carry no groups, only API permissions in `scp`, and only
when requested for an API resource (`resource=urn:sam:api`). The spike handles this:

- **Server:** the default OIDC tenant takes user roles from userinfo
  (`roles.source=userinfo`, cached per token). `ClientCredentialsTenantResolver` sends
  tokens with the API audience to an `m2m` tenant that reads roles from `scp` and enforces
  that audience. Inactive unless `sam.oidc.m2m-audience` is set.
- **UI:** `/oidc-config.json` also returns `scope` and `roleClaimPath`; `AuthService` reads
  roles from the access token and falls back to the ID token.
- **CLI:** requests its token with `grant-options.client.resource=urn:sam:api`.

User IDs (`sub`) differ per provider. Before go-live that needs no migration; afterwards,
linked musicians, shares, event-log entries and audit revisions would need remapping.

## Tasks

| # | Task | Status |
|---|------|--------|
| 1 | Land the spike: tenant resolver + `m2m` tenant, userinfo roles, `scope`/`roleClaimPath` in `/oidc-config.json`, `AuthService` fallback, `preferred_username` from userinfo, dev compose + seed script. Keycloak stays the default until task 2. | [#211](https://github.com/fhm84/sam/issues/211) |
| 2 | One setting selects the provider for server, UI config and CLI (Pocket ID default, Keycloak optional); one config block per provider; all prod URLs and secrets from env. Tests for both. | [#212](https://github.com/fhm84/sam/issues/212) |
| 3 | `UserDirectory` behind `/api/admin/users`: Pocket ID (`/api/users?search=`, API key) and Keycloak (admin client). The API key acts with admin rights and stays server-side. | [#213](https://github.com/fhm84/sam/issues/213) |
| 4 | Dev default: compose starts and seeds Pocket ID, login-link helper, dev UI reads `/oidc-config.json` from the server via the dev proxy. Keycloak dev still works. | [#214](https://github.com/fhm84/sam/issues/214) |
| 5 | `docker-compose.prod.yml`: `pocket-id` by default, `keycloak` + `keycloak-db` behind a compose profile; Caddy's login host proxies to the active provider; `.env.example` per provider. | [#215](https://github.com/fhm84/sam/issues/215) |
| 6 | Backup/restore of Pocket ID's data. Proposed: its own database in SAM's Postgres container, so the scripts keep using `pg_dump`. Restore test. | [#216](https://github.com/fhm84/sam/issues/216) |
| 7 | Production bootstrap: first admin via `/setup` with a passkey, then an idempotent env-driven seed (groups, `sam-ui`, `sam-cli` + API permission, user-directory API key). Replaces `keycloak/prod/sam-realm.json` for Pocket ID. | [#217](https://github.com/fhm84/sam/issues/217) |
| 8 | SMTP so admins can email login codes; document the recovery flow. | [#218](https://github.com/fhm84/sam/issues/218) |
| 9 | Remove the unused `ensemble:` group logic in `CurrentUserService` (membership comes from the database). | [#219](https://github.com/fhm84/sam/issues/219) |
| 10 | New ADR (Pocket ID default, Keycloak optional), ADR-0001 consequences, security concept, deployment, production setup, tech stack, `keycloak/README.md`, `cli`/`migration` READMEs, `CLAUDE.md`, open question #7. | [#220](https://github.com/fhm84/sam/issues/220) |
| 11 | Real devices on the HTTPS deployment: passkeys on iPhone, Android and a Windows PC, phone-as-key via QR code, recovery by login code, silent sign-in in a new tab. | [#221](https://github.com/fhm84/sam/issues/221) |

Order: 1 → 2 → (3, 4, 9 in any order) → 5 → 6 → 7 → 8 → 10; 11 on the real server.

## Open questions

- **#11** May users request login codes by email themselves, or only admins send them?
  Proposed: admin only at first. → [#222](https://github.com/fhm84/sam/issues/222)

## Try the spike

```bash
docker compose -f docker-compose.pocketid.yml up -d
scripts/pocketid/seed-dev.sh                       # groups, dev users, sam-ui, sam-cli
./mvnw quarkus:dev -pl server -Dquarkus.profile=dev,pocketid
# UI (until task 4): point ui/src/main/webui/public/oidc-config.json at
# curl localhost:8080/api/oidc-config.json for the session, don't commit it; then npm start
scripts/pocketid/seed-dev.sh login-link admin1     # open the link, then http://localhost:4200
java -Dquarkus.profile=pocketid -jar cli/target/quarkus-app/quarkus-run.jar list
```

## Key files

`docker-compose.pocketid.yml`, `scripts/pocketid/seed-dev.sh`,
`server/.../security/ClientCredentialsTenantResolver.java`, `server/.../security/CurrentUserService.java`,
`server/.../OidcConfigResource.java`, `server/.../api/impl/AdminUsersResourceImpl.java`,
`ui/.../core/auth/auth.service.ts`, `ui/.../core/auth/auth.config.ts`,
`docker-compose.prod.yml`, `docker/Caddyfile`, `.env.example`, `scripts/backup/`,
`%pocketid` blocks in the `server` and `cli` `application.properties`.
