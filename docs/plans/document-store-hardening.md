# Plan: Document Store Hardening

Roadmap: [Document store hardening](../roadmap.md#document-store-hardening--planned)
· Concept: [Storage & deduplication](../architecture/concepts/storage-and-deduplication.md)
· Follow-up: [Storage encryption](storage-encryption.md) ([ADR-0011](../architecture/decisions/adr-0011-stored-document-encryption.md))

**Why now:** `DocumentStore.upload` has two confirmed bugs that matter before real users upload
files. The same changes are also the prerequisites for [storage encryption](storage-encryption.md).

## Findings (verified 2026-10-11)

1. **Silent file corruption.** The temp file is named `filename + ".tmp"` in the storage root.
   `LocalFileSystemWrapper.openForWrite` opens it with `CREATE` but **without
   `TRUNCATE_EXISTING`**, and the temp file is never deleted when the upload is a
   duplicate, when an upload policy rejects it, or when the virus scan or an I/O step
   fails. If a later upload has the same file name (common: `Klarinette 1.pdf`) and smaller
   content, it is written over the leftover file. The old tail stays, and the result is
   moved into place: the stored file differs from its recorded `sha256`, and PDF readers
   follow the old tail's `startxref`. Reproduced: `OLD-CONTENT-LONGER` overwritten with `new`
   gave `new-CONTENT-LONGER`. Two concurrent uploads with the same file name also write
   into the same temp file.
2. **Uploads to S3 fail.** `TIKA.detect(filesystem.resolve(tmp).toFile())` reads a local
   file, but `S3FileSystemWrapper.resolve` returns a pseudo-path used only for logging.
   Tika throws `FileNotFoundException`, so every upload fails when `s3://` storage is configured.
3. **Leftover temp files** (see 1) pile up in the storage root. On S3 they stay in the bucket.
4. `UploadContext` receives the already-consumed upload stream, which is of no use to policies.
5. `deleteIfUnlinked` deletes the physical file **before** the database row, in the same
   transaction. If the transaction rolls back, the row survives but its file is gone.
6. Storage paths are the content's SHA-256 hash (`ab/cd/ef/<sha256>.<ext>`). That
   reveals whether a known file is stored, and the file is moved after hashing (on S3,
   a move is a copy plus a delete). Encryption needs a path that never changes after
   the write ([ADR-0011](../architecture/decisions/adr-0011-stored-document-encryption.md)).

## Tasks

| # | Task | Status |
|---|------|--------|
| 1 | Use a unique temp name (UUID, in a `.tmp/` prefix) instead of the user's file name. Local `openForWrite` uses `CREATE_NEW` (or `TRUNCATE_EXISTING`). Fixes 1. | pending |
| 2 | Always remove the temp file (`try`/`finally`): when the upload is a duplicate, rejected by a policy, or fails during the scan or I/O. Fixes 3. | pending |
| 3 | Detect the MIME type from the start of the stream (Tika on a mark/reset buffer plus the file-name hint) while the upload is written. Remove the `resolve().toFile()` call. Fixes 2. | pending |
| 4 | Drop the stream from `UploadContext` (it carries file name, size and MIME type only), or replace it with the header bytes if a policy needs content. | pending |
| 5 | Store new files under random paths (`<uuid[0:2]>/<uuid[2:4]>/<uuid>`) decoupled from the hash, and write them there directly. If the content turns out to be a duplicate, delete the new file. Upload no longer needs `move`. `sha256` stays in the database for deduplication. Existing rows keep their paths, so no data migration is needed. Fixes 6. | pending |
| 6 | Delete the physical file only **after commit** (transaction-synchronised callback), never before the row. Fixes 5. | pending |
| 7 | Remove `resolve(String)` from the `FileSystemWrapper` interface (or document it as debug-only), so no code reads files without going through the stream API. ArchUnit rule: no `filesystem.resolve` in `server`. | pending |
| 8 | Integrity check: an admin-triggered job (endpoint or CLI) that recomputes each stored file's SHA-256, compares it with the database, and reports mismatches, missing files, orphaned files and leftover temp files. Run it once on existing data to find files already damaged by finding 1. It is reused for the encryption migration. | pending |
| 9 | Tests: same file name with different content stores the correct bytes; a duplicate or rejected upload leaves no temp file; upload works against a fake wrapper whose `resolve` throws (proxy for S3); delete with a rolled-back transaction keeps the file. | pending |
| 10 | Update the [storage concept](../architecture/concepts/storage-and-deduplication.md) (paths no longer content-addressed, integrity check). | pending |

Tasks 1–3 are a small, self-contained fix and can ship first as a bugfix PR.

## Key files

- `server/src/main/java/de/halbmann/sam/business/documents/controller/DocumentStore.java`
- `storage/storage-sdk/src/main/java/de/halbmann/storage/api/FileSystemWrapper.java`
- `storage/storage-local/.../LocalFileSystemWrapper.java`, `storage/storage-s3/.../S3FileSystemWrapper.java`
- `core/src/main/java/de/halbmann/sam/core/storage/upload/UploadContext.java`
