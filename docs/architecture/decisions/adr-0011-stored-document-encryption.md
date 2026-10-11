# ADR-0011: Opt-in application-level encryption of stored documents

**Status:** accepted (implementation planned, see [plan](../../plans/storage-encryption.md))

## Context

Uploaded sheet music is licensed material, and before go-live the files sit in plain
form on the `sam-data` volume (or in an S3 bucket) and in every nightly backup. The
backup archive also contains `.env` (see [Production Setup](../production-setup.md#5-backups-a-second-machine-pulls-from-the-server)).

What encryption at rest can and cannot protect against:

| Threat | Protected? |
|---|---|
| Copy of the disk, a provider snapshot, a backup on the NAS, a misconfigured bucket | **Yes**, if the key is not stored alongside the data |
| Full compromise of the running server | **No.** The server must decrypt to serve files, so the key is in its memory and environment |
| A legitimate user or share-link recipient passing a PDF on (licence leak) | **No.** Every download is plaintext. This needs [watermarking](../../roadmap.md#watermarking-on-shared--downloaded-documents--idea), access scoping and the event log |

Alternatives considered:

- **Disk encryption (LUKS) or S3 server-side encryption.** No code changes, but the
  hosting provider or the bucket owner holds the key, and backups stay plaintext.
  These are useful additions, not replacements.
- **Client-side (end-to-end) encryption.** Rejected: the server could no longer
  classify documents, render PDFs, build ZIP bundles and setlist PDFs, or serve share links.
- **A per-file data key stored in the `documents` row.** Rejected: the storage layer
  would then need database access, which couples `storage-*` to the server schema.

## Decision

- Encryption is **opt-in** via `sam.storage.encryption.enabled` (default `false`).
- An `EncryptingFileSystemWrapper` **decorates whichever storage provider is configured**
  (local or S3), inside a new `storage-encryption` module. Business code keeps using
  `openForRead`/`openForWrite` and does not change.
- **Algorithm:** Google Tink `StreamingAead` (`AES256_GCM_HKDF_1MB`). It streams, so
  memory use doesn't grow with file size. It derives a fresh key per file from a random
  salt, and authenticates the content, so tampering is detected.
- **File format:** a short SAM magic header with a version byte, followed by the Tink
  ciphertext. The file's **storage path is used as associated data**, so encrypted files
  cannot be swapped between documents. As a result, a file's path must never change
  after it has been written (see [document store hardening](../../plans/document-store-hardening.md)).
- **Key:** a Tink keyset file mounted as a Docker secret. It is **never** placed in the
  database, in `.env` or in the backup set, and it is backed up offline separately (for
  example in a password manager). The keyset may hold several keys: new files use the
  primary key, and all keys can decrypt, which is how key rotation works.
- Storage paths are **no longer derived from the content hash**, so a file name doesn't
  reveal whether the server holds a known file.

## Consequences

- If the keyset is lost, **every stored document is lost**. Restoring a backup needs the keyset as well.
- With encryption enabled, a stolen backup or bucket no longer exposes the sheet
  music. Musicians' data in the database is not covered; that needs encrypted backups (see the plan, phase A).
- Existing plaintext files remain readable during migration
  (`sam.storage.encryption.allow-plaintext-read`) until a migration job has encrypted
  them. Turning encryption off again needs the reverse migration.
- The S3 provider's local upload buffer now holds ciphertext instead of plaintext.
- `documents.size` stays the plaintext size, so downloads and `Content-Length` are unchanged.
  The CPU cost of AES-GCM is negligible for PDFs.
