# ADR-0012: Publish multi-arch images publicly to ghcr.io

**Status:** accepted

## Context

The server and UI images were only built locally (`de.halbmann/sam`, `de.halbmann/sam-ui`),
so a deployment had to build on the host or copy images over by hand. Hosting on an ARM
device (e.g. a Raspberry Pi) also needs `linux/arm64` images, and a multi-platform Jib build
can only be pushed to a registry, not loaded into a local Docker daemon. The GitHub
Container Registry is free for public packages; private packages only get the free plan's
small storage and transfer quota, which one backend image of several hundred MB would use up.

## Decision

The CI workflow (`.github/workflows/ci.yml`) publishes both images publicly to
`ghcr.io/fhm84/sam` and `ghcr.io/fhm84/sam-ui` for `linux/amd64` and `linux/arm64`, after
the tests pass. It runs on pushes to `main` (tags `latest`, `sha-<short>`) and on `v*` tags
(tags `<version>`, `sha-<short>`), and authenticates with the built-in `GITHUB_TOKEN`.
The server image uses Jib (cross-platform without emulation); the UI image uses buildx with
the Angular build stage on the build platform. Both take their tags and standard OCI labels
(title, description, source, licenses, documentation, version, revision, created) from
`docker/metadata-action`, which replaces the labels inherited from the base images (Red Hat
OpenJDK, Caddy); the UI image also carries them as index annotations, which GHCR reads for
multi-arch packages. Registry and group are set in
`application.properties`, so local builds get the same names; `docker-compose.prod.yml`
uses them directly and only takes the tag from `SAM_IMAGE_TAG` (default `latest`).

## Consequences

- A host needs only the compose file, `keycloak/prod/` and `.env`; `docker compose pull`
  deploys or updates without a registry login.
- The images are public, so they must never contain secrets or deployment-specific
  values. Everything comes from env vars and mounted files, as before.
- On first publish GitHub may create a package as private: set its visibility to
  public once in the package settings.
- Rolling back means setting `SAM_IMAGE_TAG` to an earlier `sha-<short>`.
