---
name: docker-build
description: Build and push SAM's Docker images (backend Jib image, frontend multi-stage image) and start the production stack. Use when asked to build a Docker image, push to a registry, or start the full production stack via docker-compose.prod.yml.
---

# Docker Build

## Backend image (Jib — output: `ghcr.io/fhm84/sam:latest`)

```bash
./mvnw package -Dquarkus.container-image.build=true -pl server -am
```

Registry and group are set in `application.properties`. Pushing by hand is normally not
needed: CI already publishes both images for amd64 + arm64 to `ghcr.io/fhm84/sam` and
`ghcr.io/fhm84/sam-ui` on every push to `main` (`ci.yml`, ADR-0012). A multi-platform Jib
build (`-Dquarkus.jib.platforms=linux/amd64,linux/arm64`) can only push, not load into the
local Docker daemon.

## Frontend image (multi-stage Dockerfile — output: `ghcr.io/fhm84/sam-ui:latest`)

```bash
docker build -t ghcr.io/fhm84/sam-ui:latest .
```

## Start full production stack

Copy `.env.example` → `.env` and fill in secrets first, then:

```bash
docker compose -f docker-compose.prod.yml up
```
