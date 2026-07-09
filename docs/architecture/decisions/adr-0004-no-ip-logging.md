# ADR-0004: No IP addresses in the event log

**Status:** accepted

## Context

The `event_log` table tracks read events (downloads, exports, classification,
share-link access). Storing client IP addresses would add another attribution
signal but constitutes personal data under GDPR.

## Decision

IP addresses are **not stored**. `userId` (OIDC sub) plus a snapshotted
`username` give unambiguous attribution for authenticated access; share-link
access is identified by `shareTokenId` instead.

## Consequences

- No IP-based GDPR obligations (retention, disclosure) for the event log.
- Anonymous share-link accesses cannot be distinguished from each other beyond
  the token used — accepted trade-off for an ensemble-management context.
- A retention policy for the log itself is still an open question
  ([Roadmap §8](../../roadmap.md#8-open-questions), question 9).

See [Audit & Event Log concept](../concepts/audit.md).
