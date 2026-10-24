# Plan: Role-Aware Access (RBAC Phase 4)

Roadmap: [Full role-based access control](../roadmap.md#full-role-based-access-control-rbac--in-progress)
· Tracking issue: [#129](https://github.com/fhm84/sam/issues/129) (milestone [Go-live](https://github.com/fhm84/sam/milestone/1))
· Access model: [stakeholders, Section 6](../stakeholders.md)

**Why now:** once real musicians log in, every authenticated account can see the admin
menu. Several read endpoints also return data that only the music librarian or admin
should see. Writes are already protected by `@RolesAllowed`, but reads are not scoped.

## Findings (verified 2026-10-03, rechecked 2026-10-24)

- `app-menu.ts` has no role filtering, and `app.routes.ts` only checks `authGuard`
  (logged in or not). The `admin/*` routes are reachable by any account.
- `EventLogResourceImpl` is only `@Authenticated`, so any user can read the full event log,
  including other users' usernames and download history.
- `MusiciansResourceImpl.findMusicians` returns `contact.email`, `contact.mobile` and
  `contact.notes` to every authenticated user, although the roadmap describes notes as
  "admin-visible only".
- ~~`SharesResourceImpl.create` has no role check, so any user can create a public share link.~~
  Fixed in d2d1646: creating requires `music_librarian` or `admin`.
- `AuthService` (`core/auth/auth.service.ts`) already exposes `hasRole()` and `isAdmin`,
  read from the provider's role claim and mapped through `roleGroups` from
  `/oidc-config.json` (Keycloak realm roles, Pocket ID groups). That is the building block
  for the UI work; only `musician-form.ts` uses it so far.

## Tasks

| # | Task | Status |
|---|------|--------|
| 1 | **Backend read audit**: go through every `*ResourceImpl` read method and decide per endpoint: any user / librarian+admin / admin. Record the result in `docs/features/api-surface.md`. | [#130](https://github.com/fhm84/sam/issues/130) |
| 2 | **Event log**: restrict `EventLogResource.find` to `admin` (librarian too, if wanted). The self-service "my own access history" view is part of [event log retention](event-log-retention.md). | [#131](https://github.com/fhm84/sam/issues/131) |
| 3 | **Musician contact data**: hide `email` / `mobile` / `notes` from non-librarian callers. Either use a separate DTO or have the mapper null the fields, decided via `CurrentUserService.hasRole()`. | [#132](https://github.com/fhm84/sam/issues/132) |
| 4 | **Share creation**: restrict `create` to librarian+admin. Decided: listing and revoking stay open to every authenticated user, scoped to their own shares (d2d1646). | done |
| 5 | **Role route guard**: add a `roleGuard(...roles)` in `core/auth/` and put it on `admin/*`, `uploads`, `shares` (as task 1 decides). | [#133](https://github.com/fhm84/sam/issues/133) |
| 6 | **Menu filtering**: give menu items an optional `roles` list in `app-menu.ts` and filter them through `AuthService.hasRole()`. | [#134](https://github.com/fhm84/sam/issues/134) |
| 7 | **Action visibility**: hide create/edit/delete buttons for users without write roles (sheets, collections, musicians, instrumentations, documents). | [#135](https://github.com/fhm84/sam/issues/135) |
| 8 | **Tests**: add `@TestSecurity` tests per restricted endpoint (allowed role passes, plain user gets 403), plus Angular unit tests for the guard and menu filtering. | [#136](https://github.com/fhm84/sam/issues/136) |
| 9 | **Collection visibility**: filter collection reads by `visibility` (`WHOLE_ENSEMBLE` any user; `ADMINS_ONLY`, `PRIVATE` librarian+admin), as task 1 decided. | [#243](https://github.com/fhm84/sam/issues/243) |

## Out of scope (separate follow-ups)

- A shared ensemble-context service in the frontend (selected ensemble across features).
- Conductor role in access decisions.
- Distinct simplified views per stakeholder (musician / Dirigent / guest).

## Key files

- `server/src/main/java/de/halbmann/sam/api/impl/*ResourceImpl.java`
- `server/src/main/java/de/halbmann/sam/security/Roles.java`, `CurrentUserService.java`
- `server/src/main/java/de/halbmann/sam/business/musicians/controller/MusicianMapper.java`
- `ui/src/main/webui/src/app/core/auth/` (`auth.service.ts`, `auth.guard.ts`)
- `ui/src/main/webui/src/app/app.routes.ts`, `ui/src/main/webui/src/app/layout/component/app-menu.ts`
