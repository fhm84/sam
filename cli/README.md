# CLI module

Batch tool for a running SAM instance, built on Quarkus + Picocli. It talks to the
server exclusively through the `api` module's REST interfaces (`SamResources`),
so it exercises the same contract as the Angular UI.

Primary use case: seeding a fresh environment from the converted legacy data and
snapshotting data back out — see [`../migration/README.md`](../migration/README.md)
for that end-to-end runbook.

## Build & run

```bash
./mvnw package -pl cli -am
java -jar cli/target/quarkus-app/quarkus-run.jar <command> [options]

# dev mode alternative (note: -Dquarkus.args carries the CLI arguments)
./mvnw quarkus:dev -pl cli -am -Dquarkus.args="list -v"
```

## Commands

| Command | What it does |
|---------|--------------|
| `list [-v]` | List all sheets (ID + title; `-v` adds composer, genre, part count) |
| `show <sheetId> [-j]` | Show one sheet (`-j` prints the full JSON) |
| `import <file-or-dir>... [-d]` | Import sheets from `SheetMusic` JSON files; creates the sheet, then its instrumentations |
| `importInstrument <file-or-dir>... [-d]` | Import instruments from `CreateInstrument` JSON files |
| `importPerson <file-or-dir>... [-d]` | Import persons (composers, arrangers) |
| `importMember <file-or-dir>... [-d]` | Import members; a member whose name matches exactly one existing person becomes that person's profile, several persons of that name stop the import of that file (env-specific `userId` and invite time are ignored; person files are rejected) |
| `importEnsemble <file-or-dir>... [-d]` | Import ensembles including voices and their instrument options |
| `export <dir> [-q <query>]` | Export every sheet (optionally full-text filtered) as one `SheetMusic` JSON file |
| `exportInstrument <dir>` | Export all instruments (filename = natural ID, e.g. `TROMPETE_BB.json`) |
| `exportPerson <dir>` | Export all persons (`person-*.json`) |
| `exportMember <dir>` | Export all members (`member-*.json`), stripped of env-specific `userId` and invite time |
| `exportEnsemble <dir>` | Export all ensembles with voices + options (instrument refs are natural IDs) |

Every export format is exactly what the matching import command reads back in,
so any of them round-trips into a fresh instance.

Directories are scanned one level deep (no recursion). Exit code is `0` on
success and `1` when anything failed (including partial import failures), so
the commands are scriptable.

### Import semantics

- **Additive sync — reruns are safe and complete what is missing**
  ([ADR-0019](../docs/architecture/decisions/adr-0019-cli-import-additive-sync.md)).
  Each file is matched with the target: by its `id` when it has one and that
  record exists (a round trip from `cli export`), otherwise by natural key —
  sheets by exact title + publisher (case-insensitive; same-titled sheets from
  *different* publishers are distinct records, as in the legacy data),
  instruments by ID, persons, members and ensembles by exact name. Several
  matches stop the import of that file.
  - A missing record is **created**.
  - An existing record gets the **child records it lacks**: a sheet's parts
    (instrument + part label + clef, the database's unique key) and tags, an
    ensemble's voices (by label) and their instrument options. An import that
    failed half-way is completed by running it again.
  - Its **differing fields are reported, not written** — unless
    `-u`/`--update` is given. Only fields the file has a value for count: an
    empty field means "unknown", never "clear", and `--update` keeps it.
  - **Nothing is deleted**; child records only on the target stay, and the
    fields of existing child records are not compared.

  The summary reports `X created, Y updated, Z unchanged, W failed`, plus the
  number of records whose differing fields were left alone.
- **Validation before import.** Every file is checked against the API's
  Jakarta Bean Validation rules locally (e.g. `title` must not be blank);
  invalid files are reported per violation and counted as failures.
- **`-d`/`--dry-run`** validates and compares with the target **read-only**
  and prints what would be created, added or updated (with `--update`, which
  fields would be written). It needs the server and credentials like an import.
- **Ordering matters for a fresh instance:** sheets and ensemble voice options
  reference instruments by ID and the server rejects unknown IDs — always
  `importInstrument` first, then the rest in any order.

## Debugging errors

Errors print a one-line message plus a hint by default. `--stacktrace` (works
on every command) prints the full stack trace:

```bash
java -jar cli/target/quarkus-app/quarkus-run.jar list --stacktrace
```

Uncaught errors are routed through the same handler — a clean message and exit
code 1 instead of a raw dump. No debugger or log-level changes needed.

## Server URL and authentication

Configuration lives in `src/main/resources/application.properties`. The REST
client is keyed by the `sam-api` configKey (from
`@RegisterRestClient(configKey = "sam-api")` — don't add config keyed by the
interface's class name; it would be ignored):

- **Target server**: `quarkus.rest-client."sam-api".url` (default
  `http://localhost:8080/api`).
- **Auth**: every request carries an OIDC client-credentials Bearer token for
  the `sam-cli` service account (`music_librarian` role). Token acquisition is
  lazy: nothing contacts the OIDC provider at startup, only when the first API
  call goes out (a dry run reads the target, so it needs a token too).
- **Provider**: `SAM_OIDC_PROVIDER`, `pocket-id` (default) or `keycloak` — the
  same setting the server uses. It selects the token endpoint, the dev defaults
  and, for Pocket ID, the API resource `urn:sam:api` the token is requested
  for (the role travels as an API permission in `scp`). The settings per
  provider are in `cli/src/main/resources/oidc/`. With Pocket ID the
  `sam-cli` client is created by the `pocket-id-seed` service of `docker-compose.yml`; with Keycloak
  it is defined in `keycloak/sam-realm.json` and `keycloak/configurator/sam/`.
- **Pointing at another environment** (packaged jar runs under the `prod`
  profile): set `OIDC_SERVER_URL` (issuer URL, same variable the server uses),
  `SAM_CLI_CLIENT_ID` (default `sam-cli`), and `SAM_CLI_CLIENT_SECRET`
  (required, no default). The issuer defaults to the local dev provider
  (`localhost:1411` for Pocket ID, `localhost:8180/realms/sam` for Keycloak);
  the dev-only secrets are `sam-cli-dev-secret-0000` (Pocket ID) and
  `sam-cli-secret` (Keycloak).

```bash
OIDC_SERVER_URL=https://login.example.org \
SAM_CLI_CLIENT_SECRET=***** \
java -jar cli/target/quarkus-app/quarkus-run.jar \
  -Dquarkus.rest-client.\"sam-api\".url=https://sam.example.org/api list
```

## Known limitations

- **Collections** have no commands, deliberately: collection items reference
  sheets by UUID, and sheet UUIDs change when imported into another
  environment — a naive round-trip would silently produce broken collections,
  and resolving by title instead is guessable but fragile.
- Documents/attachments are metadata-only in exported JSON and ignored on
  import; the actual files never move. See `migration/README.md`, "Known gaps".
- Member export and import strip `userId` (OIDC subject) and the invite time — re-link
  users in the target environment. Ensemble seats are not part of the member export.

## Diagnostics

`sam.cli.diagnostics.enabled=true` (system property or config) logs the
registered subcommands at startup — useful when a command mysteriously isn't
picked up (e.g. missing CDI registration). It is purely observational; command
execution happens in `CliLauncher`, which runs the Picocli top command exactly
once and exits with its return code. For error details, use `--stacktrace`
(see "Debugging errors" above).
