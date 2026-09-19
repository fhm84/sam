# Production Setup

Checklist for running SAM on a single Linux server (a small VPS or a home server).
Examples use `sam.example.org` for the app and `login.example.org` for Keycloak —
replace them with your own hostnames. Container details:
[Deployment](deployment.md); edge proxy: [ADR-0010](decisions/adr-0010-caddy-edge-proxy.md).

## What you need

| Item | Notes |
|------|-------|
| Server | Linux (Debian/Ubuntu LTS), root access, Docker + Compose plugin. **4 GB RAM is the practical minimum, 8 GB is comfortable**; 2+ vCPU; disk for the scans (a few hundred pieces is roughly 1–4 GB) |
| Domain | Two DNS names pointing at the server: one for the app, one for Keycloak |
| Public IPv4 | With a home server, check you have a real public IPv4 (not carrier-grade NAT / DS-Lite), or publish through a tunnel instead of forwarding ports |
| LLM key | `OPENAI_API_KEY` for document classification (set a spending limit in the provider's dashboard) |
| Backup target | A second machine (for example a NAS) that can pull over SSH; see step 5 |

If the server is rented, sign the provider's data processing agreement (AVV/DPA) and
prefer an EU region — SAM stores musicians' contact data.

## 1. DNS

Add two **A records** (TTL 300–3600) to the server's IPv4 address:

- `sam.example.org` (app)
- `login.example.org` (Keycloak)

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

1. Copy the repo (or at least `docker-compose.prod.yml`, `docker/`, `keycloak/prod/`) and a filled-in `.env` (from `.env.example`) to the server. Generate every `changeme` with `openssl rand -hex 24`.
2. Set `SAM_DOMAIN`, `KEYCLOAK_DOMAIN` and `ACME_EMAIL`; `OIDC_SERVER_URL` must be `https://<KEYCLOAK_DOMAIN>/realms/sam`.
3. `docker compose -f docker-compose.prod.yml up -d`. The `sam-ui` service (Caddy, `docker/Caddyfile`) is the only published service; it fetches the certificates once DNS resolves and ports 80/443 are open. Keycloak imports `keycloak/prod/sam-realm.json` on first boot only.
4. On the very first start `sam-server` may fail to reach the Keycloak issuer before Caddy has its certificate; the restart policy retries automatically, so give it a minute or two before investigating.

What the production realm import contains: roles, the `sam-ui` / `sam-backend` / `sam-cli` clients (secrets and redirect URI filled from `.env`), and the two service accounts. It deliberately has **no dev users, no dev passwords, no localhost redirect URIs** (unlike `keycloak/sam-realm.json`, which is dev-only).

**Memory limits** (`mem_limit` in the compose file, sized for an 8 GB host; idle usage measured locally):

| Service | Limit | Measured idle |
|---|---|---|
| sam-server (JVM heap 128 MB–1 GB) | 1.5 GB | about 250–300 MB |
| Keycloak (heap up to 768 MB) | 1.25 GB | about 600 MB |
| SAM Postgres | 768 MB | about 50 MB |
| Keycloak Postgres | 384 MB | about 50 MB |
| sam-ui (Caddy edge + SPA) | 256 MB | under 30 MB |

The limits add up to about 4.3 GB, leaving roughly 3.5 GB for the OS, page cache and the optional monitoring stack. On a 4 GB host, lower the `sam-server` heap (`JAVA_TOOL_OPTIONS`) and skip the monitoring stack. `sam-server` exits on out-of-memory (`ExitOnOutOfMemoryError`) so the restart policy brings it back. Not yet load-tested: heavy PDF classification during a bulk import is the likeliest memory peak, so watch `docker stats` during the first imports.

**Known gaps:**

- Keycloak has no SMTP configured, so "forgot password" mails don't work; admins set passwords in the console.
- The Keycloak admin console (`/admin`) is reachable on the login hostname. Use a strong `KEYCLOAK_ADMIN_PASSWORD`, and consider blocking `/admin*` in `docker/Caddyfile` and using an SSH tunnel.
- Verified locally: the Caddyfile validates, and Keycloak starts in production mode from the compose file (healthy, realm imported, issuer is the public `https://<KEYCLOAK_DOMAIN>/realms/sam`, dev users can't log in). Not verified: Caddy's certificate issuance and the full stack against real DNS.

## 4. Go-live check

- `https://<SAM_DOMAIN>` loads and redirects to the Keycloak login for sign-in.
- Log in to `https://<KEYCLOAK_DOMAIN>/admin` as the bootstrap admin, switch to the `sam` realm, create your own user and assign the `admin` (and `music_librarian`) role. Then use that user, not the bootstrap account.
- For the bulk import, configure the `cli` with the `sam-cli` secret from `.env` (see `migration/README.md`, "Authentication") and import the scanned archive from your own machine (see `cli/README.md`).

## 5. Backups (a second machine pulls from the server)

The backup machine **pulls**; the server never has credentials for it. A compromised server therefore cannot delete or encrypt the backup history, and no VPN or open port on the backup machine's network is needed (it only makes outbound SSH connections).

**On the server** — `scripts/backup/backup.sh`, run nightly at 03:00 by a systemd timer:

- Dumps both Postgres databases (`sam_music`, `keycloak`), archives the sheet music files (`sam-data` volume) and copies `.env`, into `/var/backups/sam/<timestamp>/` with a `SHA256SUMS` file. The directory is renamed into place only when complete, so a puller never sees a half-written backup.
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

**Restore** — `scripts/backup/restore.sh <backup-dir> --yes` verifies the checksums, stops the app, replaces both databases and the files, and starts the stack again. On a fresh server, first do steps 2 and 3 (up to a running stack), copy a backup directory over from the backup machine, then run it. **Restore test:** do it once before go-live on a scratch machine, and again now and then.

A provider's snapshots are not a backup — they live in the same account.

**Verified locally** (Docker Desktop, test databases and volume): backup, pruning to the newest N, wiping the data, and restoring database rows and files from the backup. **Not verified:** the systemd timer, the `rrsync`-restricted key and the pull task, which need a real server and backup machine.

## Related

- [Deployment](deployment.md) — images, Caddy edge proxy, environment variables
- [Security](concepts/security.md) — OIDC / Keycloak setup
