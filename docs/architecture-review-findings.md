# Architecture Review Findings — 2026-07-01

Whole-project architecture review (module structure, layering, cross-cutting
concerns). Ranked most-severe first. Use this as a backlog; tick items off as
they're resolved.

Overall verdict: the architecture is sound — real module boundaries, an
ArchUnit test enforcing the JAX-RS-free business layer, consistent
boundary/controller/entity slicing, clean storage SPI, centralized identity
(`CurrentUserService`) and config keys (`EnvConsts`). The findings below are
the exceptions, not the rule.

---

## 1. S3 storage backend unreachable (dead module)  ✅ (21195e5)
- **Files:** `server/pom.xml`, `storage/storage-s3/.../S3FileSystemProvider.java`
- **Severity:** High (documented feature didn't exist at runtime)
- **Problem:** `server` depended only on `storage-local`; `S3FileSystemProvider`
  could never be discovered by CDI. Additionally the provider injected an
  `S3Client` for which no producer existed anywhere — adding the module naively
  would have failed ArC build-time validation.
- **Fix applied:** Added `storage-s3` to server; provider now creates its
  `S3Client` lazily in `create()` via the AWS default chain (`AWS_REGION`,
  credentials, `AWS_ENDPOINT_URL_S3` for MinIO-style stores). Scheme of
  `sam.filesystem.base.path` selects the provider.
- **Deliberate trade-off:** plain AWS SDK default chain instead of the
  `quarkus-amazon-s3` extension — keeps `storage-s3` framework-agnostic like
  `storage-local`. Revisit if `quarkus.s3.*` config or S3 dev services are wanted.

## 2. Split package `de.halbmann.sam.storage` across core and server  ✅ (21195e5)
- **Files:** `core/.../sam/storage/**` (6 files), `server/.../sam/storage/**`
- **Severity:** High (JPMS-hostile, cross-module same-package access)
- **Problem:** `de.halbmann.sam.storage(.upload)` existed in both the core and
  server jars. `FileTypePolicy` (server) implemented `UploadPolicy` (core)
  relying on *same-package* visibility across module boundaries.
- **Fix applied:** Moved core's classes to `de.halbmann.sam.core.storage.*`;
  the `de.halbmann.sam.storage` package now exists only in server.

## 3. `DocumentsService` god class  ⬜
- **File:** `server/src/main/java/de/halbmann/sam/business/documents/controller/DocumentsService.java`
- **Severity:** Medium (maintainability; ~800 lines, ~30 public methods)
- **Problem:** Four responsibilities in one class: content-addressed persistence
  with refCounting (`save`, `deleteIfUnlinked`), attachment linking
  (`linkDocument`, `linkAttachmentTo*`, `unlinkAttachments`), ZIP bundling
  (`buildZip*`), and PDF merging (`buildMergeEntries*`, `buildMergedPdf`).
  `uniqueZipName` is duplicated in `SheetExportService`.
- **Fix direction:** Split into `DocumentStore` (persistence + refCount),
  `AttachmentLinkService`, and `DocumentBundleService` (zip + merged PDF);
  share the ZIP-naming helper with `SheetExportService`. Best done as a
  dedicated refactor with the existing refCount tests as a safety net.

## 4. Test coverage inverted relative to testability  ⬜
- **Files:** `core/src/test` (missing), `api/src/test` (missing)
- **Severity:** Medium (feedback-loop cost)
- **Problem:** `core` — the purest, easiest-to-test logic (fingerprinting, MIME
  detection, `TextNormalizer`, `SortFieldValidator`) — has zero tests; so does
  `api` despite `UserInfo.displayLabel()` carrying real branching. All 25 test
  classes live in `server` where tests are slowest.
- **Fix direction:** Add plain JUnit tests in `core` (no Quarkus needed) for
  `TextNormalizer`, `Hashing`, `MimeTypeDetector`, `SortFieldValidator`,
  `FingerprintService`; one small test in `api` for `UserInfo.displayLabel()`.

## 5. Ambiguous transaction boundaries  ⬜
- **Files:** 33 classes with `@Transactional` (both repositories *and* services)
- **Severity:** Low–Medium (latent correctness/perf risk)
- **Problem:** When a service calls two repositories, the service must own the
  transaction; repository-level `@Transactional` is redundant and invites
  accidental per-call transactions if a resource ever calls a repository
  directly. Class-level `@Transactional` on services also opens write
  transactions for pure reads.
- **Fix direction:** Own transactions at the service layer only; strip
  repository annotations. Consider method-level annotations on mutating
  service methods instead of class-level.

## 6. Config and code hygiene  ✅
- **Severity:** Low
- **Items:**
  - `ClassificationService`: hardcoded debug flag → now `sam.classification.debug`
    config property (default `false`), registered in `EnvConsts`.
  - `application.properties`: GCP project ID replaced with `${GCP_PROJECT_ID:}`.
  - `server/pom.xml`: unused `aws-java-nio-spi-s3.version` property removed.

## 7. Documentation drift  ✅
- **File:** `docs/architecture.md` §6, root `CLAUDE.md`
- **Severity:** Low
- **Problem:** Tech-stack table still said Quarkus 3.33.1.1 / Spotless plugin
  3.3.0; commit `ce13c21` (2026-06-27) moved these to 3.37.0 / 3.7.0.
- **Fix applied:** Table now pins major lines only (3.37.x, 21.x, …) with a note
  that poms / `package.json` are authoritative; CLAUDE.md no longer cites an
  exact Spotless version.

## 8. Accepted trade-offs (watch, no action yet)  ⬜
- **Coverage snapshots have no invalidation** — editing instrumentations or
  ensemble voices leaves stale badges until a manual
  `POST .../coverage/compute`. Cheap middle ground: dirty-flag on writes to
  instrumentations/voices. Users will report this staleness as a bug eventually.
- **API DTO packages are named `entity`** (`api/.../entity/...`) while JPA
  entities live in `server/.../entity/` — recurring reading tax; rename only
  if a large API refactor happens anyway.
