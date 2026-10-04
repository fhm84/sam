# Plan: Attachment Metadata (Kind & Versioning)

Roadmap: [Sheet metadata enrichment](../roadmap.md#sheet-metadata-enrichment--in-progress)
(the sheet-level fields have shipped; this is the remaining attachment part)
· Feature: [Documents & attachments](../features/documents.md)

**Why:** the sheet detail design distinguishes clean, marked-up and photocopied parts
and shows v1/v2/v3 file lineage. Attachments currently only have `AttachmentType`.

## Decisions

- **Kind** is a separate `AttachmentKind` field. Do **not** extend `AttachmentType`.
- Derivative quality tiers (master / musician / preview) are generated on demand from one
  master and are not stored as extra attachments. Versioning is about replaced *sources*,
  not derivatives.

## Tasks

| # | Task | Status |
|---|------|--------|
| 1 | `AttachmentKind` enum (`CLEAN` / `MARKED_UP` / `PHOTOCOPY` / `FACSIMILE` / `SCORE`), nullable `kind` on `AttachmentEntity` / `Attachment` + migration. | pending |
| 2 | `version` (Integer) + `replacedById` (self-FK) on `AttachmentEntity` + migration. Upload "replace" sets the lineage. | pending |
| 3 | Angular: kind selector and version badge on attachment rows, "replace file" action, and older versions shown collapsed. | pending |
| 4 | Cover image upload for collections (leftover from collection fields): `coverImageId` exists but nothing sets it. | pending |

## Deferred

- Duplicate / merge / split sheet actions (the "More" menu in the v2 design).
- `uploadedBy` user reference.

## Key files

- `api/src/main/java/de/halbmann/sam/api/entity/documents/Attachment.java`
- `server/src/main/java/de/halbmann/sam/business/documents/entity/AttachmentEntity.java`
- `ui/src/main/webui/src/app/features/sheets/` (`sheet-detail.*`, `instrumentation-documents.*`)
- `ui/src/main/webui/src/app/features/collections/` (cover image)
