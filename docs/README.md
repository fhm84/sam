# SAM Documentation

**S**heet music **A**rchiving & **M**anagement — documentation map.

The docs are organised as small, single-topic pages linked to each other
(arc42-inspired structure, browsable in Obsidian or on GitHub — plain relative
Markdown links only, no wikilinks).

## Architecture

Technical structure, loosely following [arc42](https://arc42.org/) chapters —
used as a menu, not a form.

- [Context & Scope](architecture/context.md) — what SAM is, system context, quality goals
- [Building Blocks](architecture/building-blocks.md) — Maven modules and their dependencies
- [Data Model](architecture/data-model.md) — entities, relationships, key fields
- [Deployment](architecture/deployment.md) — container images, nginx, environment variables
- [Technology Stack](architecture/tech-stack.md) — major technology lines

### Cross-Cutting Concepts

- [API Design](architecture/concepts/api-design.md) — API-first shared interfaces, sub-resource pattern
- [Storage & Deduplication](architecture/concepts/storage-and-deduplication.md) — content-addressed documents, sheet fingerprinting
- [Search](architecture/concepts/search.md) — full-text, trigram, and phonetic search infrastructure
- [Coverage Evaluation](architecture/concepts/coverage.md) — ensembles, voices, playability scoring
- [AI Classification](architecture/concepts/classification.md) — document classification workflow
- [Security](architecture/concepts/security.md) — OIDC, roles, identity resolution
- [Audit & Event Log](architecture/concepts/audit.md) — Envers audit trail, read-event logging

### Runtime Views

- [Key flows](architecture/runtime/README.md) — PlantUML sequence diagrams for the four core workflows
- [architecture.puml](architecture/architecture.puml) — component diagram

### Decisions

- [Architecture Decision Records](architecture/decisions/README.md) — recorded design decisions (ADRs)

## Features

Living reference of the current feature set — one page per domain area.

- [Feature index](features/README.md)

## People & Planning

- [Stakeholders, Use Cases & Flows](stakeholders.md) — who uses SAM and how; access-control model
- [Roadmap & Ideas](roadmap.md) — planned features, ideas, open questions

## Reviews

Point-in-time review backlogs; tick items off as they're resolved.

- [Architecture review 2026-07](reviews/architecture-review-findings.md)
- Code review 2026-06 (`reviews/code-review-findings.md` — local-only, gitignored)
- Security review 2026-06 (`reviews/security-review-findings.md` — local-only, gitignored)
