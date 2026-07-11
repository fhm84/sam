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
| `import <file-or-dir>... [-d]` | Import sheets from `SheetMusic` JSON files, one sheet per file; creates the sheet, then its instrumentations |
| `importInstrument <file-or-dir>... [-d]` | Import instruments from `CreateInstrument` JSON files |
| `export <dir> [-q <query>]` | Export every sheet (optionally filtered by full-text query) as one `SheetMusic` JSON file per sheet — the same format `import` reads |

Directories are scanned one level deep (no recursion). `-d`/`--dry-run` only
checks that each file deserializes into the expected DTO — it makes no network
call and does not run server-side validation.

Exit code is `0` on success and `1` when anything failed (including partial
import failures), so the commands are scriptable.

**Ordering matters for a fresh instance:** sheets reference instruments by ID and
the server rejects unknown IDs — always `importInstrument` before `import`.

**Import is not idempotent:** re-importing the same files creates duplicate
sheets (nothing deduplicates by title).

## Server URL and authentication

Configuration lives in `src/main/resources/application.properties`. The REST
client is keyed by the `sam-api` configKey (from
`@RegisterRestClient(configKey = "sam-api")` — don't add config keyed by the
interface's class name; it would be ignored):

- **Target server**: `quarkus.rest-client."sam-api".url` (default
  `http://localhost:8080/api`).
- **Auth**: every request carries an OIDC client-credentials Bearer token for
  the `sam-cli` Keycloak client (service account with the `music_librarian`
  realm role — defined in `keycloak/sam-realm.json` and
  `keycloak/configurator/sam/`). Token acquisition is lazy: nothing contacts
  Keycloak at startup or during dry-runs, only when a real API call goes out.
- **Pointing at another environment** (packaged jar runs under the `prod`
  profile): set `OIDC_SERVER_URL` (Keycloak realm URL, same variable the server
  uses), `SAM_CLI_CLIENT_ID` (default `sam-cli`), and `SAM_CLI_CLIENT_SECRET`.
  The defaults match the local dev Keycloak (`localhost:8180/realms/sam`,
  dev-only secret `sam-cli-secret`) so local usage works out of the box —
  real deployments must override at least the secret.

```bash
OIDC_SERVER_URL=https://auth.example.com/realms/sam \
SAM_CLI_CLIENT_SECRET=***** \
java -jar cli/target/quarkus-app/quarkus-run.jar \
  -Dquarkus.rest-client.\"sam-api\".url=https://sam.example.com/api list
```

## Known limitations

- Sheets round-trip (`export` → `import`), including instrumentations; there is
  **no instrument export** yet — `importInstrument` relies on JSON files you
  already have (e.g. from the migration module).
- Documents/attachments are metadata-only in exported JSON and ignored on
  import; the actual files never move. See `migration/README.md`, "Known gaps".
- No musician/ensemble/collection commands.

## Diagnostics

`sam.cli.diagnostics.enabled=true` (system property or config) logs the
registered subcommands at startup — useful when a command mysteriously isn't
picked up (e.g. missing CDI registration). It is purely observational; command
execution happens in `CliLauncher`, which runs the Picocli top command exactly
once and exits with its return code. Log verbosity for the
`de.halbmann.sam.cli.diagnostics` category can be tuned with
`-Dsam.cli.diagnostics.level=DEBUG`.
