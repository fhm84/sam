<img src="ui/src/main/webui/public/logo.svg" width="96" align="right" alt="SAM logo">

# SAM — Sheet music Archiving & Management

[![PMD](https://github.com/fhm84/sam/actions/workflows/pmd.yml/badge.svg)](https://github.com/fhm84/sam/actions/workflows/pmd.yml)
[![Java](https://img.shields.io/badge/dynamic/xml?url=https://raw.githubusercontent.com/fhm84/sam/main/pom.xml&query=//%2A%5Blocal-name%28%29=%27maven.compiler.release%27%5D&label=Java&logo=openjdk&logoColor=white&color=ED8B00)](https://openjdk.org/projects/jdk/25/)
[![Quarkus](https://img.shields.io/badge/dynamic/xml?url=https://raw.githubusercontent.com/fhm84/sam/main/pom.xml&query=//%2A%5Blocal-name%28%29=%27quarkus.platform.version%27%5D&label=Quarkus&logo=quarkus&logoColor=white&color=4695EB)](https://quarkus.io)
[![Angular](https://img.shields.io/github/package-json/dependency-version/fhm84/sam/@angular/core?filename=ui/src/main/webui/package.json&label=Angular&logo=angular&logoColor=white&color=DD0031)](https://angular.dev)
[![PostgreSQL 17](https://img.shields.io/badge/PostgreSQL-17-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org)

A Quarkus-based application for archiving sheet music, managing instrumentations, musicians, and collections for bands and ensembles.

> [!IMPORTANT]
> **SAM manages sheet music — it grants no rights to it.** Using SAM does not
> permit anyone to scan, reproduce, store, distribute, or share copyrighted
> sheet music. Operators and users of a SAM instance are solely responsible for
> the copyright and licensing status of everything they upload or share, in
> particular scanned PDFs and other digital copies. Only store and share
> material that is in the public domain, appropriately licensed, or covered by
> a specific permission. See [Copyright and Licensing](#copyright-and-licensing).

## Quick Start

```bash
# Build all modules
./mvnw package

# Start PostgreSQL and the dev login provider (Pocket ID on :1411, seeded with dev users)
docker compose up -d

# Run server in dev mode (Dev UI at http://localhost:8080/q/dev/)
./mvnw quarkus:dev -pl server

# Log in: open a one-time login link, then http://localhost:4200 (users: admin1, librarian1, musician1)
docker compose run --rm pocket-id-seed login-link admin1

# Run tests
./mvnw test

# Format code
./mvnw spotless:apply
```

**Prerequisites:** Java 25, Node.js (22.22.3+, 24.15+ or 26+ — Angular 22's requirement; CI and the Docker build use 26), Docker, PostgreSQL 17 (with `pg_trgm` and `fuzzystrmatch` extensions), default connection: `localhost:5432/sam_music`, user `sam` (`docker compose up -d` starts one). Keycloak instead of Pocket ID in dev: `docker compose -f docker-compose.keycloak.yml up -d` and `-Dsam.oidc.provider=keycloak`.

## Features

An overview — each area links to its page in the [feature reference](docs/features/README.md).

- **[Sheet music](docs/features/sheets.md)** — title, composer, arranger, publisher, genre, difficulty, ISWC, GEMA work number and more; [search](docs/features/search-discovery.md) with full-text, trigram (fuzzy) and phonetic matching; content fingerprints against duplicates
- **[Instrumentations](docs/features/instrumentations.md)** — the parts of a sheet, each linked to the [instrument catalogue](docs/features/instruments.md) (transposition, clef, notation type), with physical location and condition
- **[Documents](docs/features/documents.md)** — upload with a staging area, content-addressed storage (SHA-256 deduplication) on the local filesystem or S3, attachments typed as full score, part, cover, lyrics, MIDI, audio, …
- **[Members](docs/features/members.md)** — the band's members with contact details, instruments and login
- **[People](docs/features/people.md)** — composers, arrangers and everyone else credited on sheets
- **[Collections & setlists](docs/features/collections.md)** — folders and dated concert programmes, with a table-of-contents PDF and a GEMA setlist export; *booklets are planned*
- **[Ensembles & coverage](docs/features/ensembles-coverage.md)** — weighted voice definitions and a score of how well the line-up can play a sheet (Complete / Playable / Incomplete)
- **AI assistance** — [classification](docs/features/ai-classification.md) of uploads (text extraction first, vision fallback for scans), [metadata enrichment](docs/features/ai-enrichment.md), and a [setlist assistant](docs/features/ai-setlist-assistant.md), via LangChain4j (OpenAI or a local Ollama)
- **Access** — OIDC login ([Pocket ID by default, Keycloak optional](docs/architecture/concepts/security.md)), roles for admins and music librarians, [share links](docs/features/shares.md) for guests, and [My Parts](docs/features/my-parts.md) for musicians
- **Audit** — full change history via Hibernate Envers, plus an admin-only [event log](docs/features/event-log.md) of downloads, exports, and AI classification runs
- **CLI** — import and export of sheets, instruments, musicians, and ensembles over the REST API (see [cli/README.md](cli/README.md))

The endpoints are summarised in the [API surface](docs/features/api-surface.md).

## Roadmap

Planned work lives in the [roadmap](docs/roadmap.md): the **Now / Next / Later**
priorities at the top, followed by the backlog of ideas and the open questions.
Items on the Now and Next lists are tracked as GitHub issues in the
[Go-live](https://github.com/fhm84/sam/milestone/1) and
[Everyday use](https://github.com/fhm84/sam/milestone/2) milestones; their task
breakdowns are in the [implementation plans](docs/plans/README.md).

## Architecture

See the [documentation map](docs/README.md) for the full docs — [module overview](docs/architecture/building-blocks.md), [data model](docs/architecture/data-model.md), [design decisions](docs/architecture/decisions/README.md), and [technology stack](docs/architecture/tech-stack.md).

## Module Structure

```
sam (parent)
 +-- api            Contracts: JAX-RS interfaces, DTOs, enums
 +-- core           Shared business logic (exceptions, utilities)
 +-- storage        Storage abstraction (SPI + local/S3 impls)
 +-- server         Quarkus runtime: REST impls, JPA entities, services
 +-- ui             Angular frontend (served via Quarkus Quinoa)
 +-- cli            PicoCLI batch import/export tool (REST client, see cli/README.md)
 +-- migration      Legacy-data conversion for the cli import tool (see migration/README.md)
```

## Configuration

### Production Deployment

The production stack (`docker-compose.prod.yml`) and **all its environment
variables** are described in [Deployment](docs/architecture/deployment.md);
server, DNS, backups, and the go-live checklist are in
[Production Setup](docs/architecture/production-setup.md).

### Application Config (`application.properties`)

SAM-specific config keys are defined as constants in `server/.../EnvConsts.java`.

| Key | Default | Description |
|-----|---------|-------------|
| `sam.filesystem.base.path` | `/data/sam` (prod, env `SAM_FILESYSTEM_BASE_PATH`) | Storage root for uploaded files. See [Storage Backends](#storage-backends). |
| `sam.files.types` | *(allow all)* | Optional file-type filter applied on upload. Whitelist: `pdf\|xml`; blacklist (prefix `^`): `^exe\|bat`. |
| `sam.oidc.provider` | `pocket-id` | Login provider, `pocket-id` or `keycloak` (env `SAM_OIDC_PROVIDER`). |
| `sam.coverage.base-score` | `0.7` | Minimum score (0–1) awarded to an ensemble voice with any positive instrumentation match. |
| `sam.classification.agentic` | `false` | Enable agentic LLM entity resolution pass after AI classification. |
| `sam.admin.keycloak.realm` | `sam` (env `KEYCLOAK_REALM`) | Keycloak only: realm queried by the admin user-search endpoint. |

### Storage Backends

`sam.filesystem.base.path` selects the backend automatically from its URI scheme:

| Value | Backend | Notes |
|-------|---------|-------|
| `/data/sam-docs` (bare path) | Local filesystem | Equivalent to `file:///data/sam-docs` |
| `s3://my-bucket/sam-docs` | AWS S3 | Requires AWS SDK credentials (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION` or `quarkus.s3.*` properties) |

### LLM Providers

Set `quarkus.langchain4j.chat-model.provider` and enable exactly one provider:

| Provider value | Required additional config |
|----------------|---------------------------|
| `openai` (default) | `quarkus.langchain4j.openai.api-key=<key>` (prod: env `OPENAI_API_KEY`); model defaults to `gpt-4o` |
| `ollama` | `quarkus.langchain4j.ollama.base-url=http://localhost:11434`; `quarkus.langchain4j.ollama.chat-model.model-id=llava` |
| `vertexai` (Gemini) | Not built by default: uncomment the `quarkus-langchain4j-vertex-ai-gemini` dependency in `server/pom.xml`, then set `quarkus.langchain4j.vertexai.gemini.project-id=<id>` and `.location=<region>` |

## Copyright and Licensing

SAM is software for cataloguing and managing sheet music. It does not
provide any rights to the musical works, sheet music editions, lyrics,
recordings, or other files managed with the application. The authors and
contributors of SAM have no control over the content stored in or shared
from any SAM instance and accept no responsibility for it.

The operator of a SAM instance is responsible for:

- verifying the copyright and licensing status of each uploaded work;
- obtaining permission to scan or otherwise digitise sheet music;
- restricting access and downloads where required;
- ensuring that shared collections and public links comply with the
  applicable licence terms;
- deleting material when the applicable permission or licence expires;
- deciding whether documents may be sent to an external AI provider: AI
  classification sends the extracted text or cropped header and footer of
  page 1 to the configured LLM (a local Ollama keeps them on your own
  infrastructure);
- reporting public performances to the collecting society (e.g. GEMA) —
  SAM's GEMA setlist export only prepares the list.

Do not upload, distribute, or share copyrighted sheet music unless the
required permission or licence has been obtained.

The software licence of SAM is separate from the rights in any content
stored in a SAM instance.

For Germany, see for example:

- [MIZ: Noten vervielfältigen, bearbeiten, veröffentlichen und verbreiten](https://miz.org/de/tutorials/noten-vervielfaeltigen-bearbeiten-veroeffentlichen-und-verbreiten)
- [VG Musikedition: Vervielfältigungen](https://vg-musikedition.de/nutzer/vervielfaeltigungen)
- [frag-amu.de: Digitale Noten](https://frag-amu.de/digitale-noten/)

This section is general information, not legal advice.

## Contributing

### Commit Messages

Commits follow [Conventional Commits](https://www.conventionalcommits.org/): `<type>(<scope>): <subject>`.

- **Types:** `build`, `chore`, `ci`, `docs`, `feat`, `fix`, `perf`, `refactor`, `revert`, `style`, `test`
- **Scope:** optional, usually a module (`ui`, `core`, `api`, `server`, `cli`, `storage`) or area (`docs`, `arch`); comma-separated for multiple (`test(core,api): ...`)
- Examples: `fix(ui): pin transitive deps to patch Dependabot security alerts`, `test(core,api): add plain JUnit tests for pure business logic`

Enable local validation once per clone:

```bash
git config core.hooksPath .githooks
```

Pull requests are also checked by the `commit-lint` CI job (`.github/workflows/ci.yml`); both call the shared `scripts/check-commit-msg.sh`.
