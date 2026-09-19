# Technology Stack

Exact versions live in the poms and `ui/src/main/webui/package.json` — those are
authoritative; the table below only pins the major lines.

| Concern | Technology | Version |
|---------|-----------|---------|
| Runtime | Quarkus | 3.39.x |
| Language | Java | 25 |
| ORM | Hibernate ORM + Panache | (via Quarkus BOM) |
| Audit | Hibernate Envers | (via Quarkus BOM) |
| Database | PostgreSQL | 17 (pg_trgm, fuzzystrmatch) |
| Migrations | Flyway | (via Quarkus BOM) |
| REST | JAX-RS (RESTEasy) | (via Quarkus BOM) |
| Serialization | JSON-B | (via Quarkus BOM) |
| AI | Quarkus LangChain4j | 1.13.x |
| Frontend | Angular (+ CDK) | 22.x |
| Frontend UI | Optimus UI (`@openng/optimus-ui`, Aura preset) | 2.x |
| Frontend language / styling | TypeScript, Tailwind CSS | 6.0 / 3.x |
| Frontend tooling | Node.js (CI, Docker) | 26 |
| Auth | Keycloak (OIDC) | 26.x |
| CLI | PicoCLI (Quarkus ext.) | (via Quarkus BOM) |
| Code Gen | Lombok, MapStruct | (see parent pom) |
| TS Gen | typescript-generator-maven-plugin | (in ui module, `-Pgenerate-ts`) |
| Formatting | Palantir Java Format (Spotless) | (see parent pom) |
| Container (backend) | Jib | (via Quarkus ext.) |
| Container (frontend / edge) | Caddy + multi-stage Dockerfile | caddy 2 |

## Related

- [Building Blocks](building-blocks.md) — which module uses what
- [Deployment](deployment.md) — how the images are built
