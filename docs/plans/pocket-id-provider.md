# Plan: Pocket ID as Default OIDC Provider

Roadmap: [Pocket ID as default OIDC provider](../roadmap.md#pocket-id-as-default-oidc-provider--in-progress)
· Tracking issue: [#210](https://github.com/fhm84/sam/issues/210)
· Concept: [Security](../architecture/concepts/security.md)
· Decision: [ADR-0016](../architecture/decisions/adr-0016-pocket-id-default-provider.md) (supersedes ADR-0003)

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
| Musician ↔ user linking (`/api/admin/users`) | ✅ since task 3 (`PocketIdUserDirectory`); Pocket ID user ID = `sub` |

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
| 3 | `UserDirectory` behind `/api/admin/users`: Pocket ID (`/api/users?search=`, API key) and Keycloak (admin client). The API key acts with admin rights: server env only, never logged, rotation documented. Optional: without it, the UI links a musician by pasting the user's `sub` (open question #14). | [#213](https://github.com/fhm84/sam/issues/213) |
| 4 | Dev default: compose starts and seeds Pocket ID, login-link helper, dev UI reads `/oidc-config.json` from the server via the dev proxy. Keycloak dev still works. | [#214](https://github.com/fhm84/sam/issues/214) |
| 5 | `docker-compose.prod.yml`: `pocket-id` by default, `keycloak` + `keycloak-db` behind a compose profile; Caddy's login host proxies to the active provider and, for Keycloak, blocks `/admin` (admin console via internal network or SSH tunnel only, open question #15); `.env.example` per provider. | [#215](https://github.com/fhm84/sam/issues/215) |
| 6 | Backup/restore of Pocket ID's data. Proposed: its own database in SAM's Postgres container, so the scripts keep using `pg_dump`. Restore test. | [#216](https://github.com/fhm84/sam/issues/216) |
| 7 | Production bootstrap: first admin via `/setup` with a passkey, then an idempotent env-driven seed (groups `sam-admin` / `sam-music-librarian` / `sam-member`, `sam-ui` restricted to them, `sam-cli` + API permission, user-directory API key). Replaces `keycloak/prod/sam-realm.json` for Pocket ID. | [#217](https://github.com/fhm84/sam/issues/217) |
| 8 | SMTP so admins can email login codes (self-service requests stay off, open question #11); document the recovery flow. | [#218](https://github.com/fhm84/sam/issues/218) |
| 9 | Remove the unused `ensemble:` group logic in `CurrentUserService` (membership comes from the database). | [#219](https://github.com/fhm84/sam/issues/219) |
| 10 | New ADR (Pocket ID default, Keycloak optional), ADR-0001 consequences, security concept, deployment, production setup, tech stack, `keycloak/README.md`, `cli`/`migration` READMEs, `CLAUDE.md`, open question #7. | [#220](https://github.com/fhm84/sam/issues/220) |
| 11 | Real devices on the HTTPS deployment: passkeys on iPhone, Android and a Windows PC, phone-as-key via QR code, recovery by login code, silent sign-in in a new tab. | [#221](https://github.com/fhm84/sam/issues/221) |

Order: 1 → 2 → (3, 4, 9 in any order) → 5 → 6 → 7 → 8 → 10; 11 on the real server.

## Open questions

Decided 2026-10-19 as proposed:

- **#11** ~~May users request login codes by email themselves, or only admins send them?~~
  **Decided:** admin only at first (about 5 active users); revisit if it becomes a burden.
  `EMAIL_ONE_TIME_ACCESS_AS_UNAUTHENTICATED_ENABLED` stays off. → [#222](https://github.com/fhm84/sam/issues/222), task 8.

From the security comparison of providers (2026-10-18). Pocket ID is often shared single
sign-on for several self-hosted apps, so SAM must not assume it is the only client:

- **#12** ~~Should the default tenant enforce the token audience?~~ **Resolved (task 2):**
  yes, for both providers. Pocket ID puts the client ID into a user's access token
  (`aud: [sam-ui, <issuer>]`), so its default tenant requires `sam-ui` (the configured UI
  client ID). Keycloak puts no SAM audience into its tokens (user tokens have none, the
  `cli` token has `account`), so the realm's `sam-ui` and `sam-cli` clients now add
  `urn:sam:api` (protocol mapper `sam-api-audience`) and Keycloak's default tenant requires
  that. Checked in dev: a token Pocket ID issued to another client, and a Keycloak
  `sam-backend` token, are both rejected with 401.
- **#13** ~~How do SAM's groups stay apart from other apps' groups?~~ **Resolved (task 2):**
  `sam.oidc.role-groups.<role>` maps each SAM role to a provider group (Pocket ID:
  `sam-admin`, `sam-music-librarian`; Keycloak: no mapping, its realm roles are SAM's).
  `RoleGroupsAugmentor` (server) and `AuthService` (UI, via `roleGroups` in
  `/oidc-config.json`) grant roles only from those groups. The dev seed adds `sam-member`
  for everyone else and restricts `sam-ui` to the three groups; the production seed
  (task 7) must do the same.
- **#14** ~~Is a full-admin Pocket ID API key on the SAM server acceptable for the user
  search?~~ **Decided:** acceptable with limits. Pocket ID API keys cannot be scoped, so a
  compromised SAM server becomes a takeover of every app behind that Pocket ID (Keycloak's
  admin client can be limited to `view-users`). So the key lives only in the server's env
  and is never logged; its rotation is documented; and user search is optional: without
  a key, an admin links a musician by pasting the user's `sub`. Task 3.
- **#15** ~~Keycloak profile: should Caddy block `/admin` on the public login host?~~
  **Decided:** yes, the admin console is reachable only from the internal network or an
  SSH tunnel. Task 5.

## Try it in dev

```bash
docker compose up -d                               # Postgres + Pocket ID, seeded on first start
./mvnw quarkus:dev -pl server                      # Pocket ID is the dev default
docker compose run --rm pocket-id-seed login-link admin1   # open the link, then http://localhost:4200
java -jar cli/target/quarkus-app/quarkus-run.jar list      # dev secret is the default
```

The dev UI gets `/oidc-config.json` from the server through the dev proxy
(`ui/src/main/webui/proxy.conf.json`), so switching provider needs no UI change. Keycloak
in dev: `docker compose -f docker-compose.keycloak.yml up -d` and
`./mvnw quarkus:dev -pl server -Dsam.oidc.provider=keycloak` (log in as `admin1` / `admin1`).

## Key files

`docker-compose.yml` (`pocket-id`, `pocket-id-seed`), `scripts/pocketid/seed-dev.sh`, `ui/src/main/webui/proxy.conf.json`,
`server/.../security/ClientCredentialsTenantResolver.java`, `server/.../security/CurrentUserService.java`,
`server/.../OidcConfigResource.java`, `server/.../api/impl/AdminUsersResourceImpl.java`,
`ui/.../core/auth/auth.service.ts`, `ui/.../core/auth/auth.config.ts`,
`docker-compose.prod.yml`, `docker/Caddyfile`, `.env.example`, `scripts/backup/`,
`OidcProviderConfigSourceFactory` and `oidc/{pocket-id,keycloak}.properties` in `server` and `cli`
(`sam.oidc.provider`, env `SAM_OIDC_PROVIDER`).
