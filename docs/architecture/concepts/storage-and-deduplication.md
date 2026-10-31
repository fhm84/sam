# Storage & Deduplication

## Document Storage

Each document's file lives at a **random path**, `<uuid[0:2]>/<uuid[2:4]>/<uuid>`, written once and never moved: the path says nothing about the content, and an encrypting store can bind the ciphertext to it ([ADR-0011](../decisions/adr-0011-stored-document-encryption.md)). Rows from before this change keep their content-addressed paths (`ab/cd/ef/<sha256>.<ext>`); both kinds are read the same way.

**Deduplication stays on the content:** `DocumentStore.upload` streams the file through the virus scanner and a SHA-256 digest straight to its new path, detects the MIME type by reading it back through the wrapper, and runs the upload policies (`UploadContext`: file name, size, MIME type). If a document with the same `sha256` exists, the new copy is deleted and the existing document is returned; its reference count is raised by whoever attaches it. The `attachments` table links documents to sheets/instrumentations with typed metadata.

**Files follow the transaction** (`StoredFileCleanup`, a JTA synchronisation): a deleted document's file is removed only after the commit, so a rollback keeps row and file; a new file whose row rolls back is removed too. A delete that fails at that point is logged and leaves an orphan.

**Integrity check:** `POST /api/admin/storage/integrity-check` (admin; the *Storage integrity* card on the Configuration page) re-hashes every document's file and compares hash and size with the database, and lists files no document points to and upload staging files of older versions (`.upload-tmp/…`, or `<name>.tmp` in the root from before the temp-file fix). It only reports. Run it while nobody uploads (an upload in flight shows as an orphan), once after updating to find files the old temp-file bug damaged, and before and after the encryption migration.

Application code reaches file content only through the `FileSystemWrapper` streams (`openForWrite`, `openForRead`, `list`, …); the interface has no way to get a local `Path`, and `ArchitectureTest` keeps code off the local wrapper's `resolve`. So upload, download and the check behave the same on local disk and S3.

The storage layer uses an SPI (`FileSystemProvider` / `FileSystemWrapper`) so backends can be swapped between local filesystem and S3 without changing business logic. Both providers ship in the server image; `FileSystemWrapperResolver` picks the one matching the `sam.filesystem.base.path` scheme (bare path → local, `s3://bucket/prefix` → S3). The S3 client is created lazily only when an `s3://` path is configured, using the AWS default chain (`AWS_REGION`, `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`, `AWS_ENDPOINT_URL_S3` for S3-compatible stores like MinIO).

## Fingerprinting & Deduplication (sheets)

Each sheet music entry gets a deterministic fingerprint computed from its metadata (title, composer, etc.) at persist time via `@PrePersist`. A unique constraint on the fingerprint column prevents duplicate entries.

Key classes: `FingerprintService` / `FingerprintFactory`.

## Related

- [ADR-0011](../decisions/adr-0011-stored-document-encryption.md) — opt-in encryption of stored documents (planned: [encryption](../../plans/storage-encryption.md))
- [Building Blocks](../building-blocks.md) — the `storage-sdk` / `storage-local` / `storage-s3` modules
- [Data Model](../data-model.md) — `documents` and `attachments` entities
- [Documents & Attachments feature](../../features/documents.md)
