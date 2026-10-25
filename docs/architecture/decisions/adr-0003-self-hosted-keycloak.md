# ADR-0003: Self-hosted Keycloak 26 as OIDC provider

**Status:** superseded by [ADR-0016](adr-0016-pocket-id-default-provider.md):
Pocket ID is the default, Keycloak stays supported as the alternative. The
`ensemble:{UUID}` groups below were never used and are gone; ensemble membership
lives in the database.

## Context

SAM needed an OIDC provider for authentication. Options: self-hosted
(Keycloak) or SaaS (Auth0, Google).

## Decision

Self-hosted **Keycloak 26**, run via `docker-compose.keycloak.yml` in dev
(realm auto-imported from `keycloak/sam-realm.json`) and as an external service
in production. Realm roles `admin` / `music_librarian`; per-ensemble access
modelled as `ensemble:{UUID}` groups in the JWT `groups` claim.

## Consequences

- No per-user SaaS costs; data stays with the ensemble (GDPR-friendly).
- The ensemble operator must run and upgrade a Keycloak instance.
- The Angular app discovers the Keycloak URL at runtime via
  `/oidc-config.json`, so images are deployment-agnostic.
- Realm config exists in two representations (full export + configurator
  split) that currently must be kept in sync manually — see `keycloak/README.md`.

See [Security concept](../concepts/security.md).
