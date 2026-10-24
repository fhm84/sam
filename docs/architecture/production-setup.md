# Production Setup

Checklist for running SAM on a single Linux server (a small VPS or a home server).
Examples use `sam.example.org` for the app and `login.example.org` for the login provider
(Pocket ID by default, or Keycloak) — replace them with your own hostnames. Container details:
[Deployment](deployment.md); edge proxy: [ADR-0010](decisions/adr-0010-caddy-edge-proxy.md).

## What you need

| Item | Notes |
|------|-------|
| Server | Linux (Debian/Ubuntu LTS), root access, Docker + Compose plugin. **4 GB RAM is the practical minimum, 8 GB is comfortable**; 2+ vCPU; disk for the scans (a few hundred pieces is roughly 1–4 GB) |
| Domain | Two DNS names pointing at the server: one for the app, one for the login provider |
| Public IPv4 | With a home server, check you have a real public IPv4 (not carrier-grade NAT / DS-Lite), or publish through a tunnel instead of forwarding ports |
| LLM key | `OPENAI_API_KEY` for document classification (set a spending limit in the provider's dashboard) |
| Backup target | A second machine (for example a NAS) that can pull over SSH; see step 5 |

If the server is rented, sign the provider's data processing agreement (AVV/DPA) and
prefer an EU region — SAM stores musicians' contact data.

## 1. DNS

Add two **A records** (TTL 300–3600) to the server's IPv4 address:

- `sam.example.org` (app)
- `login.example.org` (login: Pocket ID or Keycloak)

Create them as plain DNS records. If the domain also has a website on shared web hosting,
do **not** create the names as hosting/control-panel subdomains — that would create a
website there instead of pointing at your server. Existing records (website, MX, SPF)
stay untouched. If the zone has CAA records, they must allow `letsencrypt.org`.

## 2. Harden the server

- Firewall: allow only 22, 80, 443.
- SSH by key only, no root password login.
- Enable automatic security updates.
- Add a 2 GB swapfile as an OOM safety net.
- Install Docker from Docker's own apt repository (includes the Compose plugin).

## 3. Deploy the stack

1. Copy the repo (or at least `docker-compose.prod.yml`, `docker/`, and for Keycloak `keycloak/prod/`) and a filled-in `.env` (from `.env.example`) to the server. Generate every `changeme` with `openssl rand -hex 24` (`POCKET_ID_ENCRYPTION_KEY`: `openssl rand -base64 32`, and never change it afterwards).
2. Set `SAM_DOMAIN`, `LOGIN_DOMAIN` and `ACME_EMAIL`. Choose the login provider with `SAM_OIDC_PROVIDER` (`pocket-id`, the default, or `keycloak`) and fill in only that provider's section of `.env`. `COMPOSE_PROFILES=${SAM_OIDC_PROVIDER}` (already in `.env.example`) makes compose start only that provider. With Keycloak, `OIDC_SERVER_URL` must be `https://<LOGIN_DOMAIN>/realms/sam`; Pocket ID's issuer is the login host itself, which is the default.
3. `docker compose -f docker-compose.prod.yml pull` fetches the published images (no registry login needed; optionally pin `SAM_IMAGE_TAG`, see [Deployment](deployment.md)). Then `docker compose -f docker-compose.prod.yml up -d`. To update later: `pull`, then `up -d` again. The `sam-ui` service (Caddy, `docker/Caddyfile`) is the only published service; it fetches the certificates once DNS resolves and ports 80/443 are open. It proxies the login host to the active provider (both answer as `oidc:8080` on the internal network) and answers `/admin` there with 404, so Keycloak's admin console and admin API are reachable only through an SSH tunnel to the server.
4. On the very first start `sam-server` may fail to reach the provider's issuer before Caddy has its certificate; the restart policy retries automatically, so give it a minute or two before investigating.
5. Set up the provider (below).

**Pocket ID first setup** (until an automated, env-driven setup lands: [#217](https://github.com/fhm84/sam/issues/217)). Do step 1 right after the first start: until an admin exists, whoever opens the setup page first becomes admin.

1. Open `https://<LOGIN_DOMAIN>/setup` and create your admin account with a passkey.
2. *Settings → Administration → User Groups*: create `sam-admin`, `sam-music-librarian` and `sam-member` (exactly these names: the first two grant SAM's roles, `sam-member` lets everyone else sign in, because `sam-ui` admits only these three groups).
3. *OIDC Clients → Add*: name `SAM`, **public** client, custom client ID `sam-ui` (or set `OIDC_CLIENT_ID` to the generated one), callback URL and logout callback URL `https://<SAM_DOMAIN>`; then allow only the three groups above.
4. For the bulk import with the `cli`: an OIDC client with custom ID `sam-cli` (confidential; note its secret), and under *APIs* an API with resource `urn:sam:api`, a permission `music_librarian`, and client access for `sam-cli` with that permission.
5. Optional: an API key (*API Keys*) for the musician–user search, set as `SAM_OIDC_POCKET_ID_API_KEY` (see [security](concepts/security.md) for what it can do and how to rotate it).
6. Create the users, put them into the groups, and send each a login code (*Users*, then the user's menu) to register their passkey.

**Keycloak** imports `keycloak/prod/sam-realm.json` on first boot only: roles, the `sam-ui` / `sam-backend` / `sam-cli` clients (secrets and redirect URI filled from `.env`), and the two service accounts. It deliberately has **no dev users, no dev passwords, no localhost redirect URIs** (unlike `keycloak/sam-realm.json`, which is dev-only).

**Memory limits** (`mem_limit` in the compose file, sized for an 8 GB host; idle usage measured locally):

| Service | Limit | Measured idle |
|---|---|---|
| sam-server (JVM heap 128 MB–1 GB) | 1.5 GB | about 250–300 MB |
| SAM Postgres | 768 MB | about 50 MB |
| sam-ui (Caddy edge + SPA) | 256 MB | under 30 MB |
| Pocket ID (default provider) | 256 MB | about 20 MB |
| Keycloak (heap up to 768 MB; `keycloak` profile only) | 1.25 GB | about 600 MB |
| Keycloak Postgres (`keycloak` profile only) | 384 MB | about 50 MB |

With Pocket ID the limits add up to about 2.8 GB (with Keycloak: about 4.2 GB), leaving the rest of an 8 GB host for the OS, page cache and the optional monitoring stack. On a 4 GB host, lower the `sam-server` heap (`JAVA_TOOL_OPTIONS`) and skip the monitoring stack. `sam-server` exits on out-of-memory (`ExitOnOutOfMemoryError`) so the restart policy brings it back. Not yet load-tested: heavy PDF classification during a bulk import is the likeliest memory peak, so watch `docker stats` during the first imports.

**Known gaps:**

- No SMTP is configured yet ([#218](https://github.com/fhm84/sam/issues/218)): Pocket ID admins hand out login codes as links; Keycloak's "forgot password" mails don't work, so admins set passwords in the console.
- Keycloak's admin console: reach it through an SSH tunnel, for example a temporary `ports: ["127.0.0.1:8180:8080"]` on the `keycloak` service plus `ssh -L 8180:localhost:8180 <server>`, then `http://localhost:8180/admin`.
- Verified locally: the Caddyfile validates; with internal certificates (`*.localhost`), Caddy's login host reaches Pocket ID and Keycloak (healthy; issuer `https://<LOGIN_DOMAIN>` and `…/realms/sam`) and answers `/admin` with 404; `sam-server` in production mode accepts Pocket ID user tokens (roles from userinfo) and the `cli` token, and searches users with the API key; backup and restore run with Pocket ID. Not verified: Caddy's certificate issuance and the full stack against real DNS.

## 4. Go-live check

- `https://<SAM_DOMAIN>` loads and redirects to the provider's login for sign-in.
- Pocket ID: put your admin account into `sam-admin` (and `sam-music-librarian`) and sign in to SAM with it.
- Keycloak: open the admin console through the SSH tunnel (see above) as the bootstrap admin, switch to the `sam` realm, create your own user and assign the `admin` (and `music_librarian`) role. Then use that user, not the bootstrap account.
- For the bulk import, configure the `cli` with the `sam-cli` secret from `.env` (see `migration/README.md`, "Authentication") and import the scanned archive from your own machine (see `cli/README.md`).

## 5. Backups (a second machine pulls from the server)

The backup machine **pulls**; the server never has credentials for it. A compromised server therefore cannot delete or encrypt the backup history, and no VPN or open port on the backup machine's network is needed (it only makes outbound SSH connections).

**On the server** — `scripts/backup/backup.sh`, run nightly at 03:00 by a systemd timer:

- Dumps the Postgres databases (`sam_music`, and `keycloak` with the Keycloak profile), exports Pocket ID's data with its own `pocket-id export` (`pocket-id.zip`: users, passkeys, groups, clients; consistent while it runs), archives the sheet music files (`sam-data` volume) and copies `.env`, into `/var/backups/sam/<timestamp>/` with a `SHA256SUMS` file. The directory is renamed into place only when complete, so a puller never sees a half-written backup.
- Keeps the newest 7 backups locally; the backup machine keeps its own longer history.
- `.env` contains secrets, so the backup directory is readable only by root and the `sam-backup` group.

```bash
# one-time setup on the server (repo checked out at /opt/sam, see step 3)
sudo useradd --system --create-home --shell /bin/bash sam-backup
sudo install -d -m 750 -g sam-backup /var/backups/sam
sudo cp /opt/sam/scripts/backup/systemd/sam-backup.* /etc/systemd/system/
sudo systemctl daemon-reload && sudo systemctl enable --now sam-backup.timer
sudo systemctl start sam-backup.service   # run once now, then check /var/backups/sam
```

**On the backup machine** (for a NAS: its task scheduler or cron) — a nightly task at about 04:00, running as a user with its own SSH key:

```bash
rsync -a --exclude='.tmp-*' -e "ssh -i ~/.ssh/sam_backup -o IdentitiesOnly=yes" \
  sam-backup@sam.example.org:/var/backups/sam/ /path/to/backups/sam/
```

- No `--delete`: the backup machine keeps backups after the server has pruned them. Prune its copy yourself (for example keep 30 dailies and a few monthlies).
- Put the backup user's **public** key in `/home/sam-backup/.ssh/authorized_keys` on the server, restricted to read-only rsync: `restrict,command="rrsync -ro /var/backups/sam" ssh-ed25519 AAAA… backup`. (`rrsync` ships with the `rsync` package; on Debian/Ubuntu it may need to be enabled or copied from the package docs.) In that case use `sam-backup@…:/` as the source path.

**Restore** — `scripts/backup/restore.sh <backup-dir> --yes` verifies the checksums, stops the app, replaces the databases in the backup, Pocket ID's data (`pocket-id import`, if the backup has it) and the files, and starts the stack again. On a fresh server, first do steps 2 and 3 (up to a running stack), copy a backup directory over from the backup machine, then run it. **Restore test:** do it once before go-live on a scratch machine, and again now and then.

A provider's snapshots are not a backup — they live in the same account.

**Verified locally** (Docker Desktop, test databases and volume): backup, pruning to the newest N, wiping the data, and restoring database rows, files and Pocket ID users from the backup. The systemd timer and the backup run on a Debian 13 server. **Not verified:** the `rrsync`-restricted key and the pull task, which need a backup machine.

## Related

- [Deployment](deployment.md) — images, Caddy edge proxy, environment variables
- [Security](concepts/security.md) — OIDC providers, user search, API key rotation
