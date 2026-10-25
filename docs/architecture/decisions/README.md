# Architecture Decision Records

One small file per decision. Format: Context → Decision → Consequences.
Status values: `accepted` · `superseded` · `deferred`.

The initial set below was retro-documented on 2026-07-09 from decisions
previously recorded in the roadmap's open-questions table and plan notes.

| ADR | Decision | Status |
|-----|----------|--------|
| [0001](adr-0001-musician-user-linking.md) | Link user accounts via `Musician.userId` — no separate User entity | accepted |
| [0002](adr-0002-resource-scoped-share-tokens.md) | Guest access via resource-scoped share tokens, not visibility flags | accepted |
| [0003](adr-0003-self-hosted-keycloak.md) | Self-hosted Keycloak 26 as OIDC provider | superseded by 0016 |
| [0004](adr-0004-no-ip-logging.md) | No IP addresses in the event log | accepted |
| [0005](adr-0005-single-name-field.md) | Keep single `name` field on Musician (no first/last split) | accepted |
| [0006](adr-0006-collection-type-enum.md) | Keep `FOLDER`/`SETLIST` collection type enum; UI maps richer labels onto it | accepted |
| [0007](adr-0007-management-interface.md) | Separate management interface (`:9000`) for `/q/*` ops endpoints | accepted |
| [0008](adr-0008-assistant-ensemble-scoping.md) | AI assistant tools never receive authorization-relevant arguments | accepted |
| [0009](adr-0009-version-controlled-prompts.md) | Prompts are version-controlled resources, not runtime config | accepted |
| [0010](adr-0010-caddy-edge-proxy.md) | Caddy replaces nginx as the single edge proxy in the UI image; `/q/*` not exposed publicly | accepted |
| [0011](adr-0011-stored-document-encryption.md) | Opt-in application-level encryption of stored documents; key kept outside database and backups | accepted |
| [0012](adr-0012-public-images-on-ghcr.md) | CI publishes multi-arch (amd64 + arm64) images publicly to ghcr.io | accepted |
| [0013](adr-0013-readiness-gates-startup.md) | Readiness check (database + storage) gates the edge's start; no liveness-driven restarts | accepted |
| [0014](adr-0014-mermaid-diagrams.md) | Diagrams in the docs are inline Mermaid blocks; ASCII art and PlantUML files are replaced | accepted |
| [0015](adr-0015-person-catalogue-and-member-profile.md) | Person catalogue for credits plus an optional member profile for band members (shared id) | accepted |
| [0016](adr-0016-pocket-id-default-provider.md) | Pocket ID as the default OIDC provider; Keycloak stays supported | accepted |

New decisions: add the next number, keep it short (context, decision,
consequences — a screen or less), and link it from this table.
