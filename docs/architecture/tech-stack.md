# Technology Stack

Exact versions live in the poms and `ui/src/main/webui/package.json` — those are
authoritative; the table below only pins the major lines.

| Concern | Technology | Version |
|---------|-----------|---------|
| Runtime | Quarkus | 3.38.x |
| Language | Java | 25 |
| ORM | Hibernate ORM + Panache | (via Quarkus BOM) |
| Audit | Hibernate Envers | (via Quarkus BOM) |
| Database | PostgreSQL | (pg_trgm, fuzzystrmatch) |
| Migrations | Flyway | (via Quarkus BOM) |
| REST | JAX-RS (RESTEasy) | (via Quarkus BOM) |
| Serialization | JSON-B | (via Quarkus BOM) |
| AI | LangChain4j (Quarkus ext.) | 1.7.x |
| Frontend | Angular | 21.x |
| Frontend UI | PrimeNG (Aura preset) | 21.x |
| CLI | PicoCLI (Quarkus ext.) | (via Quarkus BOM) |
| Code Gen | Lombok, MapStruct | (see parent pom) |
| TS Gen | typescript-generator-maven-plugin | (in ui module, `-Pgenerate-ts`) |
| Formatting | Palantir Java Format (Spotless) | (see parent pom) |
| Container (backend) | Jib | (via Quarkus ext.) |
| Container (frontend) | nginx + multi-stage Dockerfile | alpine |

## Related

- [Building Blocks](building-blocks.md) — which module uses what
- [Deployment](deployment.md) — how the images are built
