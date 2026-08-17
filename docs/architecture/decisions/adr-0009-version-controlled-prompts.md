# ADR-0009: Prompts are version-controlled resources, not runtime config

**Status:** accepted

## Context

The four AI service interfaces (`SheetAnalyzer`, `ClassificationAgent`,
`SetlistAssistant`, `ProgrammeTextDrafter`) originally carried their prompts as
Java text blocks in `@SystemMessage`/`@UserMessage` annotations — hard to read
and edit as prose. Externalizing them raised the question of whether prompts
should become a live-editable config surface (env vars, database, admin UI).

## Decision

Prompts live as plain-text resources under `server/src/main/resources/prompts/`,
loaded via LangChain4j's `fromResource`. They are **code**: changed only through
a commit and code review, never editable at runtime.

## Consequences

- Prompts are readable/editable as prose and diffable in review.
- The tool-grounding and anti-injection rules they contain cannot be weakened
  by a config change or compromised admin account — a prompt change ships like
  any other code change, through review and a release.
- Trade-off: prompt tuning requires a redeploy; accepted, since prompt changes
  should be tested like code changes anyway.

See [ADR-0008](adr-0008-assistant-ensemble-scoping.md).
