# Shares & Public Access

Resource-scoped share tokens allow specific content to be accessed by unauthenticated users via a URL.
Design rationale: [ADR-0002](../architecture/decisions/adr-0002-resource-scoped-share-tokens.md).

## Share token

A share token links one authenticated creator to one target resource (a single instrumentation, a whole sheet, or a collection). Tokens can carry an optional expiry date and can be revoked at any time.

| Field | Type | Notes |
|-------|------|-------|
| Creator | User ID (OIDC sub) | The authenticated user who created the token |
| Resource type | Enum (`ShareType`) | `INSTRUMENTATION` · `SHEET` · `COLLECTION` |
| Resource ID | UUID | The specific resource being shared |
| Expires at | DateTime | Optional; `null` = no expiry |
| Revoked at | DateTime | Set on revocation; `null` = active |

## Share management (authenticated)

`GET /api/shares` · `POST /api/shares` · `DELETE /api/shares/{id}`

The Angular **shares** page lists all tokens created by the current user, showing resource label, creation date, expiry, and status. Actions: **copy link** (copies the public URL to clipboard), **revoke** (immediately invalidates the token).

## Public access (unauthenticated)

The guest opens `/share/{token}` (Angular route, no login). It calls these unauthenticated
endpoints, all of which validate the token (unknown → 404; expired/revoked → only the title):

| Endpoint | Returns |
|----------|---------|
| `GET /api/public/shares/{token}` | Share metadata (`PublicShareInfo`) for the landing page |
| `GET /api/public/shares/{token}/download` | Instrumentation: the part (single file or ZIP). Sheet/collection: ZIP of all parts (`?type=`, default `PART`) |
| `GET /api/public/shares/{token}/toc` | Collection only: table-of-contents PDF |
| `GET /api/public/shares/{token}/download/{instrumentationId}` | Sheet/collection: one part, only if it belongs to the shared resource |
| `GET /api/public/shares/{token}/sheet/{attachmentId}` | Sheet only: one sheet-level attachment |

The Angular **public-share** page renders:
- For an **instrumentation**: sheet title, composer, instrument name, part label, and download links for attached documents.
- For a **sheet**: title, composer, its instrumentations, sheet-level attachments, and a download-all action.
- For a **collection**: programme order, titles, composers, per-part downloads, the TOC, and a download-all action.

## Access logging

Every public-share request is logged in `event_log` with `shareTokenId` set and `userId`/`username` as `null`. The event log UI shows "via share link" with the token ID as a tooltip.

## Related

- [Event Log](event-log.md) — where access is recorded
- [Stakeholders](../stakeholders.md) — guest persona (S5) and share use cases (UC-N6, UC-G1/G2)
