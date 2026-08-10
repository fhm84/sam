---
name: docker-build
description: Build and push SAM's Docker images (backend Jib image, frontend multi-stage image) and start the production stack. Use when asked to build a Docker image, push to a registry, or start the full production stack via docker-compose.prod.yml.
---

# Docker Build

## Backend image (Jib — output: `de.halbmann/sam:latest`)

```bash
./mvnw package -Dquarkus.container-image.build=true -pl server -am
```

Push to a registry (append registry/group overrides as needed):

```bash
./mvnw package -Dquarkus.container-image.build=true -Dquarkus.container-image.push=true \
  -Dquarkus.container-image.registry=ghcr.io \
  -Dquarkus.container-image.group=your-org \
  -pl server -am
```

## Frontend image (multi-stage Dockerfile — output: `de.halbmann/sam-ui:latest`)

```bash
docker build -t de.halbmann/sam-ui:latest .
```

## Start full production stack

Copy `.env.example` → `.env` and fill in secrets first, then:

```bash
docker compose -f docker-compose.prod.yml up
```
