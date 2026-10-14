# Plan: Encryption of Stored Documents

Roadmap: [Encryption of stored documents](../roadmap.md#encryption-of-stored-documents--planned)
· Tracking issue: [#162](https://github.com/fhm84/sam/issues/162) (milestone [Go-live](https://github.com/fhm84/sam/milestone/1))
· Decision: [ADR-0011](../architecture/decisions/adr-0011-stored-document-encryption.md)
· Depends on: [Document store hardening](document-store-hardening.md) (tasks 3, 5, 7, 8)

**Goal:** a copy of the storage volume, an S3 bucket or a backup should not expose the
sheet music. This does not protect against a compromised running server or against
licence leaks by legitimate users. See the ADR for the threat model.

## Phase A: operations (no code, worth doing before go-live)

Tracked in [#162](https://github.com/fhm84/sam/issues/162). Phase B gets issues once it moves to the roadmap's Now or Next list.

| # | Task | Status |
|---|------|--------|
| A1 | Encrypt the backup archives on the server with [age](https://age-encryption.org/), using a **public key**. The private key stays offline. Today the backup holds the files, both database dumps **and `.env`** in plaintext. Update `scripts/backup/backup.sh`, `restore.sh` and the [production setup](../architecture/production-setup.md#5-backups-a-second-machine-pulls-from-the-server). | [#163](https://github.com/fhm84/sam/issues/163) |
| A2 | Production setup: mention the provider's volume encryption or LUKS for the data volume as an optional extra layer. | [#164](https://github.com/fhm84/sam/issues/164) |

## Phase B: in-app encryption (opt-in feature)

| # | Task | Status |
|---|------|--------|
| B1 | New module `storage/storage-encryption`: `EncryptingFileSystemWrapper` decorates any `FileSystemWrapper` and uses Tink `StreamingAead` (`AES256_GCM_HKDF_1MB`). The file is a magic header with a version byte, then the ciphertext. The relative path is the associated data. `move` decrypts and re-encrypts (or is rejected); after hardening task 5 it is not used for uploads. | pending |
| B2 | Configuration: `sam.storage.encryption.enabled` (default `false`), `sam.storage.encryption.keyset-file`, `sam.storage.encryption.allow-plaintext-read` (default `true` while migrating). Wire it into `FileSystemWrapperResolver`. Startup fails if encryption is enabled and the keyset can't be read. The key is never logged. | pending |
| B3 | Key handling: document generating and rotating the keyset with `tinkey`; mount it as a Docker secret in `docker-compose.prod.yml`; keep it **out of** `.env` and the backup script; add a production-setup section on storing the key offline and testing a restore with it. | pending |
| B4 | Migration job (admin-triggered, idempotent, resumable): for each plaintext document, write the encrypted copy to a new random path, update `documents.path` in the same transaction, and delete the old file after commit. Run the integrity check (hardening task 8) before and after. | pending |
| B5 | Key rotation: add a new primary key to the keyset (old files remain readable), and optionally re-encrypt with the B4 job. | pending |
| B6 | Tests: local round trip; a flipped byte is detected; a file copied to another path fails to decrypt; plaintext is read when allowed and rejected when not; a large file streams in bounded memory; the S3 upload buffer contains ciphertext only. | pending |
| B7 | Docs: storage concept, deployment environment variables, `features/documents.md`. Flip the roadmap status. | pending |

## Open questions

1. **Deduplication hash:** keep plain `sha256` in `documents` or, with encryption enabled,
   store an HMAC keyed from a secret? Database dumps are part of the backup, and a plain
   hash lets someone who holds a known file confirm that SAM stores it. Phase A1 already
   covers this for encrypted backups.
2. **How the migration is triggered:** admin endpoint, CLI command, or an optional startup job?
3. **Default for new installs:** stay opt-in, or switch to on by default once it has proven stable?

## Key files

- `storage/storage-sdk/src/main/java/de/halbmann/storage/api/FileSystemWrapper.java`
- `server/src/main/java/de/halbmann/sam/storage/FileSystemWrapperResolver.java`
- `server/src/main/java/de/halbmann/sam/business/documents/controller/DocumentStore.java`
- `scripts/backup/`, `docker-compose.prod.yml`, `docs/architecture/production-setup.md`
