# Architecture Decision Records

One small file per decision. Format: Context → Decision → Consequences.
Status values: `accepted` · `superseded` · `deferred`.

The initial set below was retro-documented on 2026-07-09 from decisions
previously recorded in the roadmap's open-questions table and plan notes.

| ADR | Decision | Status |
|-----|----------|--------|
| [0001](adr-0001-musician-user-linking.md) | Link user accounts via `Musician.userId` — no separate User entity | accepted |
| [0002](adr-0002-resource-scoped-share-tokens.md) | Guest access via resource-scoped share tokens, not visibility flags | accepted |
| [0003](adr-0003-self-hosted-keycloak.md) | Self-hosted Keycloak 26 as OIDC provider | accepted |
| [0004](adr-0004-no-ip-logging.md) | No IP addresses in the event log | accepted |
| [0005](adr-0005-single-name-field.md) | Keep single `name` field on Musician (no first/last split) | accepted |
| [0006](adr-0006-collection-type-enum.md) | Keep `FOLDER`/`SETLIST` collection type enum; UI maps richer labels onto it | accepted |
| [0007](adr-0007-management-interface.md) | Separate management interface (`:9000`) for `/q/*` ops endpoints | accepted |

New decisions: add the next number, keep it short (context, decision,
consequences — a screen or less), and link it from this table.
