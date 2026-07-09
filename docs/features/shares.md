# Shares & Public Access

Resource-scoped share tokens allow specific content to be accessed by unauthenticated users via a URL.
Design rationale: [ADR-0002](../architecture/decisions/adr-0002-resource-scoped-share-tokens.md).

## Share token

A share token links one authenticated creator to one target resource (a sheet instrumentation or a collection). Tokens can carry an optional expiry date and can be revoked at any time.

| Field | Type | Notes |
|-------|------|-------|
| Creator | User ID (OIDC sub) | The authenticated user who created the token |
| Resource type | Enum | `SHEET_INSTRUMENTATION` · `COLLECTION` |
| Resource ID | UUID | The specific resource being shared |
| Expires at | DateTime | Optional; `null` = no expiry |
| Revoked at | DateTime | Set on revocation; `null` = active |

## Share management (authenticated)

`GET /api/shares` · `POST /api/shares` · `DELETE /api/shares/{id}`

The Angular **shares** page lists all tokens created by the current user, showing resource label, creation date, expiry, and status. Actions: **copy link** (copies the public URL to clipboard), **revoke** (immediately invalidates the token).

## Public access (unauthenticated)

`GET /public/share/{token}` — validates the token and returns the resource. No `Authorization` header required.

The Angular **public-share** page renders:
- For a **sheet instrumentation**: instrument name, part label, archive location, condition, and download links for attached documents.
- For a **collection**: programme order, titles, composers, durations, and download links for attached documents.

## Access logging

Every public-share request is logged in `event_log` with `shareTokenId` set and `userId`/`username` as `null`. The event log UI shows "via share link" with the token ID as a tooltip.

## Related

- [Event Log](event-log.md) — where access is recorded
- [Stakeholders](../stakeholders.md) — guest persona (S5) and share use cases (UC-N6, UC-G1/G2)
