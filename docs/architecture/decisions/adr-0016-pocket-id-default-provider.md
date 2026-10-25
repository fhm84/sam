# ADR-0016: Pocket ID as the default OIDC provider; Keycloak stays supported

**Status:** accepted (supersedes [ADR-0003](adr-0003-self-hosted-keycloak.md))

## Context

[ADR-0003](adr-0003-self-hosted-keycloak.md) chose a self-hosted Keycloak. For a small
ensemble it is the heaviest part of the stack: about 425–600 MB RAM idle plus its own
Postgres, and a realm that has to be kept in sync in two representations by hand. Members
log in a few times a year, so password resets are the common support case.
[Pocket ID](https://pocket-id.org) is one Go binary (about 20 MB idle) with passkeys
instead of passwords. The spike and its trade-offs are in the
[Pocket ID plan](../../plans/pocket-id-provider.md).

## Decision

- **Pocket ID is the default** in dev and production; **Keycloak stays supported.** SAM
  speaks plain OIDC, and one setting selects the provider: `sam.oidc.provider` (env
  `SAM_OIDC_PROVIDER`) loads `oidc/<provider>.properties` in `server` and `cli`, and
  `/oidc-config.json` hands the provider's scope and role claim to the UI.
- **Roles come only from SAM's own groups** (`sam-admin`, `sam-music-librarian`;
  `sam.oidc.role-groups`), and only tokens meant for SAM are accepted, because a Pocket ID
  instance is often shared by several apps. Pocket ID puts groups only into userinfo, so
  the server reads roles from there; the `cli`'s client-credentials token carries its role
  as an API permission on `urn:sam:api` and goes to a separate `m2m` tenant.
- **Musician linking uses the active provider's user API** behind one interface
  (`UserDirectory`); with Pocket ID the API key is optional.
- **Recovery is a login code an admin sends**; users can't request one themselves.

## Consequences

- The login stack shrinks to one small container whose data is backed up with
  Pocket ID's own export (`scripts/backup/`).
- Pocket ID API keys can't be scoped: the key for the user search acts as a full admin of
  Pocket ID, so it lives only in the server's env (see [Security](../concepts/security.md)).
- Users without a passkey-capable device depend on an admin's login code.
- User IDs (`sub`) differ per provider: switching provider on a running instance means
  remapping linked musicians, shares, event-log entries and audit revisions.
- Both providers stay documented and tested; Keycloak runs as a compose profile.
