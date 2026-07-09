# ADR-0002: Guest access via resource-scoped share tokens

**Status:** accepted

## Context

Guests (concert audience, partner ensembles, unregistered musicians) need
read access to specific content without an account. Options considered:
per-sheet visibility flags, a persistent "shared" flag on collections, or
explicit per-resource tokens.

## Decision

Implement **resource-scoped share tokens** (`shares` table): one token grants
access to exactly one resource (a sheet instrumentation or a collection), with
optional expiry and explicit revocation. Any authenticated user can create and
manage their own tokens. Open/anonymous browsing without a link is deliberately
deferred.

## Consequences

- More flexible than a boolean flag: multiple independent links per resource,
  each with its own lifetime.
- The librarian must actively generate and distribute each link.
- Access is auditable per token (`event_log.shareTokenId`).
- Persistent visibility flags (`SheetCollection.visibility`) remain a separate,
  planned feature for authenticated-user scoping — not a replacement for tokens.

See [Shares & Public Access](../../features/shares.md) and
[Stakeholders §6.3](../../stakeholders.md#63-content-visibility-scoping).
