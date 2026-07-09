# Storage & Deduplication

## Content-Addressed Document Storage

Documents are stored by SHA-256 hash. Uploading the same file twice does not create a duplicate — instead, the existing document's reference count is incremented. The `attachments` table links documents to sheets/instrumentations with typed metadata.

The storage layer uses an SPI (`FileSystemProvider` / `FileSystemWrapper`) so backends can be swapped between local filesystem and S3 without changing business logic. Both providers ship in the server image; `FileSystemWrapperResolver` picks the one matching the `sam.filesystem.base.path` scheme (bare path → local, `s3://bucket/prefix` → S3). The S3 client is created lazily only when an `s3://` path is configured, using the AWS default chain (`AWS_REGION`, `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`, `AWS_ENDPOINT_URL_S3` for S3-compatible stores like MinIO).

## Fingerprinting & Deduplication (sheets)

Each sheet music entry gets a deterministic fingerprint computed from its metadata (title, composer, etc.) at persist time via `@PrePersist`. A unique constraint on the fingerprint column prevents duplicate entries.

Key classes: `FingerprintService` / `FingerprintFactory`.

## Related

- [Building Blocks](../building-blocks.md) — the `storage-sdk` / `storage-local` / `storage-s3` modules
- [Data Model](../data-model.md) — `documents` and `attachments` entities
- [Documents & Attachments feature](../../features/documents.md)
