# Implementation Plans

Task-level plans for roadmap items that are `planned` or `in progress`. The
[roadmap](../roadmap.md) says *what* and *why*; a plan here says *how*: the task
breakdown, the decisions made so far, the open questions and the key files.

Conventions:

- One page per roadmap item, named after it.
- Task tables use `pending` / `partial` / `done`. Flip the status in the same commit
  that ships the task, and flip the roadmap status when the last task is done.
- When a plan is fully shipped, delete the page. The feature reference in
  [features/](../features/README.md) and git history take over. Record lasting design
  choices as an [ADR](../architecture/decisions/README.md) rather than keeping the plan.

## Now: before go-live

| Plan | Roadmap item | Blocked on |
|---|---|---|
| [Role-aware access](rbac-role-aware-access.md) | Full RBAC (Phase 4) | — |
| [Coverage snapshot invalidation](coverage-snapshot-invalidation.md) | Automatic coverage snapshot invalidation | Open question #5 |
| [Event log retention](event-log-retention.md) | Document access log (pending part) | Open question #9 |

## Next: everyday use for the music librarian

| Plan | Roadmap item |
|---|---|
| [Classification form enhancements](classification-enhancements.md) | Classification form enhancements |
| [Sheets overview filters & bulk actions](sheets-overview.md) | Sheets overview filter & bulk actions |
| [Home dashboard](home-dashboard.md) | Home dashboard |
| [Coverage breakdown enhancements](coverage-breakdown.md) | Coverage breakdown enhancements |

## Later

| Plan | Roadmap item |
|---|---|
| [Attachment metadata](attachment-metadata.md) | Sheet metadata enrichment (attachment part) |
| [Classification queue](classification-queue.md) | Classification queue |

Most plans were derived from the Claude Design mockups (`*.html` handoff files, not in
the repo). They capture the data-model and feature gaps those mockups showed.
