# ADR-0008: AI assistant tools never receive authorization-relevant arguments

**Status:** accepted

## Context

The AI setlist assistant suggests repertoire via LLM tool-calling
(`SetlistCandidateTool`). Candidate retrieval is scoped to the collection's
ensemble so only playable pieces are suggested. If the tool took an
`ensembleId` argument, the model (steered by whatever text ends up in the
prompt — including user-supplied goals) could pass an arbitrary ID, and the
tool would have to re-validate authorization on every call.

## Decision

Authorization-relevant values are **never LLM-controlled**. The resource layer
resolves and authorizes the ensemble ID once, stores it in a request-scoped
context (`SetlistAssistantContext`), and the tool reads it from there. Tool
signatures expose only harmless search parameters to the model.

## Consequences

- Prompt injection via the free-text goal cannot widen data access: there is no
  argument through which the model could reach another ensemble's data.
- No authorization logic duplicated inside tools; it stays at the resource
  boundary like every other endpoint.
- New assistant tools must follow the same pattern: anything security-relevant
  comes from the request-scoped context, not from the model.

See [AI Setlist Assistant feature](../../features/ai-setlist-assistant.md) and
[ADR-0009](adr-0009-version-controlled-prompts.md).
