# Keycloak configuration

There are currently **two representations of the same `sam` realm config** in this
directory. They are not auto-synced — see "Keeping the two in sync" below.

## `sam-realm.json` — authoritative for local dev

A single Keycloak realm-export file. `docker-compose.keycloak.yml` mounts this
directory into the container and starts Keycloak with `start-dev --import-realm`,
which reads this file natively on container startup. This is what `docker compose
-f docker-compose.keycloak.yml up` uses today, and `--import-realm` only applies
on first boot of a fresh realm — editing this file and restarting an existing
container does **not** push the change into a running instance.

## `configurator/` — split layout for [keycloak-configurator](https://github.com/CycriLabs/keycloak-configurator)

The same realm content, broken out into the per-entity-type directory structure
that [keycloak-configurator](https://github.com/CycriLabs/keycloak-configurator)
expects (hierarchical layout, one file per entity, one subdirectory per realm):

```
keycloak/configurator/
└── sam/                    # realm name
    ├── realms/sam.json
    ├── clients/sam-backend.json
    ├── clients/sam-ui.json
    ├── realm-roles/admin.json
    ├── realm-roles/music_librarian.json
    ├── groups/ensemble-00000000-0000-0000-0000-000000000001.json
    └── users/
        ├── admin1.json
        ├── librarian1.json
        ├── musician1.json
        └── service-account-sam-backend.json
```

`keycloak/configurator/` is a dedicated root containing **only** realm-named
subdirectories — the tool expects `<mounted-root>/<realm>/<entity-type>/*.json`,
so nothing else (like this README or `sam-realm.json`) lives inside it.

Unlike `--import-realm`, `configure` is idempotent and reconciles a **running**
instance every time it's run — so it's the answer to "I changed a role/client and
want that pushed without wiping the dev realm". It's wired into
`docker-compose.keycloak.yml` as an opt-in service behind the `configure` profile
(it does not run on a plain `docker compose up`):

```bash
docker compose -f docker-compose.keycloak.yml --profile configure run --rm keycloak-configurator
```

This depends on the `keycloak` service being healthy (healthcheck added for this
purpose) and applies `keycloak/configurator/sam/**/*.json` to it via the admin
REST API on the internal Docker network.

## Keeping the two in sync

Until one of these is retired, changes to realm/client/role/group/user config
(e.g. a new realm role, a new client redirect URI) need to be applied to **both**
`sam-realm.json` and the matching file(s) under `configurator/sam/`. If this
drifts and becomes a maintenance burden, that's the signal to drop
`--import-realm` from the `keycloak` service entirely and make `configure`
(run once after every fresh `docker compose up`) the only bootstrap path —
at that point `sam-realm.json` can be deleted.
