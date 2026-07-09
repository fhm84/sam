# ADR-0001: Link user accounts via `Musician.userId` — no separate User entity

**Status:** accepted

## Context

With OIDC authentication in place, SAM needed a way to connect a login to the
domain model. Options: a dedicated `User` entity referencing `Musician`, or a
direct link on the musician record. Many musicians in the catalogue are
external or historical (composers, arrangers) and will never have a login.

## Decision

Add a nullable `userId` column (OIDC `sub` claim) to `Musician`. A musician
either has a login or doesn't — there is no separate `User` entity. Linking is
admin-only via dedicated endpoints; the general musician update intentionally
ignores `userId` so a form save can never clear an existing link.

## Consequences

- Personalisation (My Parts) resolves the caller's `sub` to a `Musician` and
  works off their ensemble memberships — no join table between users and domain.
- User metadata (email verification, credentials) stays entirely in Keycloak.
- If SAM ever needs users who are *not* musicians, this decision must be revisited.

See [Security concept](../concepts/security.md) for the implementation.
