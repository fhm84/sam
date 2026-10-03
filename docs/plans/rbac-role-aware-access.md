# Plan: Role-Aware Access (RBAC Phase 4)

Roadmap: [Full role-based access control](../roadmap.md#full-role-based-access-control-rbac--in-progress)
· Access model: [stakeholders, Section 6](../stakeholders.md)

**Why now:** once real musicians log in, every authenticated account can see the admin
menu. Several read endpoints also return data that only the music librarian or admin
should see. Writes are already protected by `@RolesAllowed`, but reads are not scoped.

## Findings (verified 2026-10-03)

- `app-menu.ts` has no role filtering, and `app.routes.ts` only checks `authGuard`
  (logged in or not). The `admin/*` routes are reachable by any account.
- `EventLogResourceImpl` is only `@Authenticated`, so any user can read the full event log,
  including other users' usernames and download history.
- `MusiciansResourceImpl.findMusicians` returns `contact.email`, `contact.mobile` and
  `contact.notes` to every authenticated user, although the roadmap describes notes as
  "admin-visible only".
- `SharesResourceImpl.create` has no role check, so any user can create a public share link.
- `AuthService` (`core/auth/auth.service.ts`) already exposes `hasRole()` and `isAdmin`
  from the token's `realm_access.roles`, which is the building block for the UI work.

## Tasks

| # | Task | Status |
|---|------|--------|
| 1 | **Backend read audit**: go through every `*ResourceImpl` read method and decide per endpoint: any user / librarian+admin / admin. Record the result in `docs/features/api-surface.md`. | pending |
| 2 | **Event log**: restrict `EventLogResource.find` to `admin` (librarian too, if wanted). The self-service "my own access history" view is part of [event log retention](event-log-retention.md). | pending |
| 3 | **Musician contact data**: hide `email` / `mobile` / `notes` from non-librarian callers. Either use a separate DTO or have the mapper null the fields, decided via `CurrentUserService.hasRole()`. | pending |
| 4 | **Share creation**: restrict `create` / `revoke` to librarian+admin (or allow musicians but only for resources they can access; decide). | pending |
| 5 | **Role route guard**: add a `roleGuard(...roles)` in `core/auth/` and put it on `admin/*`, `uploads`, `shares` (as task 1 decides). | pending |
| 6 | **Menu filtering**: give menu items an optional `roles` list in `app-menu.ts` and filter them through `AuthService.hasRole()`. | pending |
| 7 | **Action visibility**: hide create/edit/delete buttons for users without write roles (sheets, collections, musicians, instrumentations, documents). | pending |
| 8 | **Tests**: add `@TestSecurity` tests per restricted endpoint (allowed role passes, plain user gets 403), plus Angular unit tests for the guard and menu filtering. | pending |

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
