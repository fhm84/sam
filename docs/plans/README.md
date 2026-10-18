# Implementation Plans

Task-level plans for roadmap items that are `planned` or `in progress`. The
[roadmap](../roadmap.md) says *what* and *why*; a plan here says *how*: the task
breakdown, the decisions made so far, the open questions and the key files.

Conventions:

- One page per roadmap item, named after it.
- **Plans on the Now and Next lists are tracked in GitHub issues.** Each has a tracking
  issue, with one sub-issue per open task, in the
  [Go-live](https://github.com/fhm84/sam/milestone/1) (Now) or
  [Everyday use](https://github.com/fhm84/sam/milestone/2) (Next) milestone. The task
  table's status cell links the issue, and the issue's state is the status: close it from
  the pull request (`Closes #N`) and leave the table row alone. Open questions a plan needs
  answered get an issue too. A task shared by two plans has one issue, linked from both.
- When a plan moves onto the Now or Next list, create its issues the same way (label `now`
  or `next` plus `area: …`, milestone, sub-issues of the tracking issue) and put the links
  into the table. When it moves from Next to Now, relabel and move its issues.
- Plans on the Later list keep `pending` / `partial` / `done` in their task tables.
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
| [Pocket ID as default OIDC provider](pocket-id-provider.md) | Pocket ID as default OIDC provider | [#210](https://github.com/fhm84/sam/issues/210) | — (open question #11, [#222](https://github.com/fhm84/sam/issues/222), only affects task 8; security questions #12–#15 affect tasks 2, 3, 5, 7) |

## Next: everyday use for the music librarian

| Plan | Roadmap item | Issue |
|---|---|---|
| [Classification form enhancements](classification-enhancements.md) | Classification form enhancements | [#172](https://github.com/fhm84/sam/issues/172) |
| [Sheets overview filters & bulk actions](sheets-overview.md) | Sheets overview filter & bulk actions | [#182](https://github.com/fhm84/sam/issues/182) |
| [Home dashboard](home-dashboard.md) | Home dashboard | [#193](https://github.com/fhm84/sam/issues/193) |
| [Coverage breakdown enhancements](coverage-breakdown.md) | Coverage breakdown enhancements | [#166](https://github.com/fhm84/sam/issues/166) |

## Later

| Plan | Roadmap item |
|---|---|
| [Attachment metadata](attachment-metadata.md) | Sheet metadata enrichment (attachment part) |
| [Classification queue](classification-queue.md) | Classification queue |
| [Storage encryption](storage-encryption.md), phase B | Encryption of stored documents (in-app, opt-in) |
| [Tailwind CSS v4 upgrade](tailwind-v4.md) | Tailwind CSS v4 upgrade |

Most plans were derived from the Claude Design mockups (`*.html` handoff files, not in
the repo). They capture the data-model and feature gaps those mockups showed.
