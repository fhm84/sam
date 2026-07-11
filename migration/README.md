# Legacy data migration

This module converts a one-time export from the previous ("legacy") archive
system into JSON files the [`cli`](../cli) module can import into a fresh SAM
instance via REST. It exists to (re)populate both a real production instance
and disposable test environments from the same source data — see
[`../cli/README.md`](../cli/README.md) for the import commands themselves.

## Inputs

`migration/src/migration-data/` (both the raw exports and the converted
`data/` output) is gitignored — it holds real archive/publisher data, not
sample fixtures, so it never leaves this machine via git. A fresh clone starts
without it; get a copy of the directory from wherever you keep it before
running anything below.

Two raw legacy exports live directly in this module (not under `resources/`,
since they are data, not configuration):

- `src/migration-data/noten_mkn.json` — sheet music catalogue ("Noten").
  Converted today.
- `src/migration-data/ensemble_mkn.json` — musicians/ensemble roster
  ("Ensemble"). **Not converted yet** — there is currently no code path that
  reads this file. Musicians and ensemble membership need to be entered
  through the UI/API until this is built.

## Conversion: `DataConversion.shouldConvert()`

`src/test/java/.../DataConversion.java` is a JUnit test in name only — it is
really a one-off regeneration script, run manually whenever the source data
or the genre mapping changes:

```bash
./mvnw test -pl migration -am -Dtest=DataConversion -Dsurefire.failIfNoSpecifiedTests=false
```

It reads `noten_mkn.json`, maps each legacy "art" (genre) value via
[`src/main/resources/legacy-genre-mapping.json`](src/main/resources/legacy-genre-mapping.json)
(see [`docs/genre-migration.md`](docs/genre-migration.md) for the human-readable
mapping table these two must be kept in sync with), parses the `stimmen`
(instrumentation) string per sheet via `InstrumentationParser`, and writes one
JSON file per sheet/instrument to:

- `src/migration-data/data/sheets/*.json` — deserializes as `SheetMusic`
- `src/migration-data/data/instruments/*.json` — deserializes as `Instrument`

Rerunning the script first clears both output directories, so it always
regenerates a clean, consistent set of files — it is safe to run repeatedly.

**Known, intentional gap:** legacy "art" values `new`, `Traditional`, and
`Moderato` have no genre mapping on purpose (per `genre-migration.md`'s
cleanup notes) — the 25 sheets carrying those values are exported with no
`genre`/`style`/tags. This is expected, not a bug.

## Importing into a fresh SAM instance

Order matters: sheets reference instruments by ID, and the server rejects an
instrumentation whose `instrumentId` doesn't exist yet
(`InstrumentationService` throws `EntityNotFoundException`). Always import
instruments first:

```bash
# 1. instruments
./mvnw package -pl cli -am
java -jar cli/target/quarkus-app/quarkus-run.jar importInstrument migration/src/migration-data/data/instruments

# 2. sheets (creates the sheet, then its instrumentations, per file)
java -jar cli/target/quarkus-app/quarkus-run.jar import migration/src/migration-data/data/sheets
```

Point the CLI at the target server via
`quarkus.rest-client."de.halbmann.sam.api.boundary.SamResources".url`
(defaults to `http://localhost:8080/api`, see `cli/src/main/resources/application.properties`).
`cli` authenticates as the `sam-cli` Keycloak client (service account, `music_librarian`
realm role) via OIDC client-credentials — see "Authentication" below for how to
point it at a different Keycloak/realm in production.

Both commands accept `-d`/`--dry-run`. Note its actual scope: it only checks
that each file **deserializes** into the expected DTO — it does not run Jakarta
Bean Validation or any server-side rule, so a clean dry-run does not guarantee
the server will accept the record (e.g. a blank title would still be reported
as "would import").

Imports are **idempotent** — records that already exist on the target are
skipped, not duplicated (sheets match by exact title + publisher, instruments
by natural ID, musicians/ensembles by name), so rerunning an import after a
partial failure is safe. Files are also validated locally (Jakarta Bean
Validation) before anything is sent; `--dry-run` runs exactly that validation
and nothing else.

## Reusing this for a disposable test environment

This mirrors how `docker-compose.keycloak.yml`'s `keycloak-configurator`
profile reseeds a fresh Keycloak realm: stand up an empty SAM + Postgres,
then run the two `cli` commands above against it. There is currently no
single script wiring this together — the module gap is tracked at
[`docs/roadmap.md`](../docs/roadmap.md)'s "Excel / CSV import" entry, which
notes the CLI path "already supports batch import via REST, but requires
technical setup."

## Exporting data back out

`cli export <dir>` writes every sheet as one `SheetMusic` JSON file — the
same shape `cli import` reads back in and the same shape this module's
`data/sheets/` output uses — so you can snapshot a running instance and
reimport it into a fresh one without touching this migration module at all:

```bash
java -jar cli/target/quarkus-app/quarkus-run.jar export ./snapshot
# optionally scope it: --query <text> matches the same full-text search as `list`
```

Instruments, musicians, and ensembles have equivalent commands
(`exportInstrument`, `exportMusician`, `exportEnsemble` and their `import*`
counterparts) — together they snapshot everything needed to seed a fresh
instance except collections and document files (see "Known gaps").

**Metadata only, by design** — documents/attachments are deliberately out of
scope (see "Known gaps" below). This mirrors `GET /sheets/{id}/export?format=JSON`
(`SheetExportService`), which is also round-trip compatible with `cli import`
for the same reason, but is per-sheet; `cli export` is the bulk equivalent.
The ZIP export format bundles the real attachment files, but flattens
sheet-level and every instrumentation's per-part attachments into one
`documents/` folder with only the filename as identification — there's no
manifest tying a file back to which instrumentation it belonged to, so it
isn't machine-reimportable today even though the bytes are there.

## Authentication

Every `*ResourceImpl` in `server` is `@Authenticated`, and write endpoints
additionally require the `music_librarian` or `admin` realm role. `cli`
authenticates as a dedicated Keycloak client, `sam-cli` — a confidential
client with a service account, granted the `music_librarian` realm role
(same pattern as `sam-backend`'s service account; see `keycloak/README.md`).
`quarkus-rest-client-oidc-filter` acquires a client-credentials token and
attaches it as a Bearer header to every `SamResources` call automatically
(`cli/src/main/resources/application.properties`); there's no manual login
step.

- **Dev** (`quarkus:dev` or plain `mvn test`): uses the dev Keycloak
  (`localhost:8180/realms/sam`) and the dev-only secret `sam-cli-secret`,
  same convention as `sam-backend-secret`. Discovery and eager token
  acquisition are both disabled
  (`quarkus.oidc-client.discovery-enabled=false`,
  `.early-tokens-acquisition=false`), so nothing reaches out to Keycloak at
  CLI startup — only when a command actually calls the API (dry-run import
  and pure-logic tests never do, so they stay hermetic).
- **Prod**: override via `OIDC_SERVER_URL` (same variable `server` uses),
  `SAM_CLI_CLIENT_ID` (default `sam-cli`), and `SAM_CLI_CLIENT_SECRET`
  (required, no default). The `sam-cli` client and its service-account role
  grant are defined in both `keycloak/sam-realm.json` and
  `keycloak/configurator/sam/{clients,users}/`, kept in sync per
  `keycloak/README.md`'s existing convention.

Verified by obtaining a real token from a running dev Keycloak via
`grant_type=client_credentials` and confirming `realm_access.roles` contains
`music_librarian` — the exact claim path `server` reads
(`quarkus.oidc.roles.role-claim-path=realm_access/roles`).

## Known gaps

- **`ensemble_mkn.json` is still not converted** — no code reads the legacy
  musician/ensemble roster export. However, musicians and ensembles created in
  a running SAM instance now round-trip via
  `cli exportMusician`/`importMusician` and `cli exportEnsemble`/
  `importEnsemble` (ensembles include voices and instrument options;
  memberships and user links are environment-specific and excluded — re-link
  in the target). Instruments round-trip via `exportInstrument`/
  `importInstrument`. See `cli/README.md` for the full command table.
- **Collections** deliberately have no export/import: collection items
  reference sheets by UUID, which change when imported into another
  environment — a naive round-trip would silently produce broken collections.
- **Documents/attachments**: exported sheet JSON may include attachment
  *metadata* (filename, MIME type, checksum), but `cli import` ignores it and
  the actual files never move in either direction (see "Exporting data back
  out" above).
