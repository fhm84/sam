# ADR-0013: Readiness check gates startup; no liveness-driven restarts

**Status:** accepted

## Context

`sam-server` had no health check, so the edge (`sam-ui`) started as soon as the server's
container did and answered 502 while Quarkus booted and Flyway migrated. Nothing reported a
server that ran but couldn't do its job, e.g. a storage volume it can't write to.
The production stack runs on Docker Compose on a single host, not Kubernetes. Docker doesn't
restart a container because it is *unhealthy*; `restart: unless-stopped` only acts when the
process exits.

## Decision

- `quarkus-smallrye-health` serves `/q/health` on the management port ([ADR-0007](adr-0007-management-interface.md)).
- Readiness (`/q/health/ready`) covers what SAM can't work without: the database (built-in
  datasource check) and writable document storage (`StorageReadinessCheck`, writes and deletes
  a probe file through `FileSystemWrapper`).
- External services the archive can run without (OpenAI, the OIDC provider) stay out of
  readiness. Their failures show up in metrics and logs, not as an unhealthy server.
- `docker-compose.prod.yml` gives `sam-server` a healthcheck on `/q/health/ready`, and `sam-ui`
  depends on it with `service_healthy`. So `docker compose up -d` waits for the server and
  fails when it stays unhealthy.
- No liveness check of our own: nothing on a Compose host would act on it. A JVM that has run
  out of memory exits (`-XX:+ExitOnOutOfMemoryError`) and the restart policy brings it back.

## Consequences

- The edge only starts once the server can serve requests; an update with a broken volume or
  database fails at `docker compose up -d` instead of after the first upload.
- The healthcheck made a real defect visible: a new `sam-data` volume belongs to root, while
  the server runs as uid 185. The one-shot `sam-data-init` service now hands the volume's root
  to uid 185 before the server starts.
- The healthcheck only gates startup. A server that becomes unhealthy later is marked
  unhealthy in `docker ps` but keeps running and keeps receiving traffic. Alerts for that
  belong to outside-in monitoring (uptime check on the public URL, a dead-man's switch for the
  backup), which this decision doesn't cover.
- The storage probe writes a small file every 10 s. On S3 storage that is about 26,000 cheap
  requests a day; cache the result if S3 is ever used in production.
