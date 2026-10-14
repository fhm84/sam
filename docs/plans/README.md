# Implementation Plans

Task-level plans for roadmap items that are `planned` or `in progress`. The
[roadmap](../roadmap.md) says *what* and *why*; a plan here says *how*: the task
breakdown, the decisions made so far, the open questions and the key files.

Conventions:

- One page per roadmap item, named after it.
- **Plans on the Now list are tracked in GitHub issues.** Each has a tracking issue in the
  [Go-live milestone](https://github.com/fhm84/sam/milestone/1), with one sub-issue per
  open task. The task table's status cell links the issue, and the issue's state is the
  status: close it from the pull request (`Closes #N`) and leave the table row alone.
  Open questions a plan needs answered get an issue too.
- When a plan moves onto the Now list, create its issues the same way (labels `now` and
  `area: …`, milestone, sub-issues of the tracking issue) and put the links into the table.
- Plans not yet on the Now list keep `pending` / `partial` / `done` in their task tables.
  Flip the status in the same commit that ships the task.
- Flip the roadmap status when the last task is done.
- When a plan is fully shipped, delete the page. The feature reference in
  [features/](../features/README.md) and git history take over. Record lasting design
  choices as an [ADR](../architecture/decisions/README.md) rather than keeping the plan.

## Now: before go-live

| Plan | Roadmap item | Issue | Blocked on |
|---|---|---|---|
| [Role-aware access](rbac-role-aware-access.md) | Full RBAC (Phase 4) | [#129](https://github.com/fhm84/sam/issues/129) | — |
| [Coverage snapshot invalidation](coverage-snapshot-invalidation.md) | Automatic coverage snapshot invalidation | [#137](https://github.com/fhm84/sam/issues/137) | Open question #5 ([#138](https://github.com/fhm84/sam/issues/138)) |
| [Event log retention](event-log-retention.md) | Document access log (pending part) | [#146](https://github.com/fhm84/sam/issues/146) | Open question #9 ([#147](https://github.com/fhm84/sam/issues/147)) |
| [Document store hardening](document-store-hardening.md) | Document store hardening | [#154](https://github.com/fhm84/sam/issues/154) | — |
| [Storage encryption](storage-encryption.md), phase A only | Encryption of stored documents (backup encryption) | [#162](https://github.com/fhm84/sam/issues/162) | — |

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
| [Storage encryption](storage-encryption.md), phase B | Encryption of stored documents (in-app, opt-in) |
| [Tailwind CSS v4 upgrade](tailwind-v4.md) | Tailwind CSS v4 upgrade |

Most plans were derived from the Claude Design mockups (`*.html` handoff files, not in
the repo). They capture the data-model and feature gaps those mockups showed.
