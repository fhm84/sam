# Plan: Sheets Overview — Filters, Sort & Bulk Actions

Roadmap: [Sheets overview filter & bulk actions](../roadmap.md#sheets-overview-filter--bulk-actions--planned)
· Feature: [Search & discovery](../features/search-discovery.md)

**Why:** the design's filter toolbar and bulk actions go beyond what `SheetFilterRequest`
and the API support today. Related: PR #96 adds an instrumentation-count filter
("exactly 4 horns, no oboes") through the same filter request.

## Tasks

| # | Task | Status |
|---|------|--------|
| 1 | **Coverage filter**: `coverageStatus` (list) + `ensembleId` on `SheetFilterRequest`, joined via coverage snapshots. Only meaningful once [snapshot invalidation](coverage-snapshot-invalidation.md) keeps them fresh. | pending |
| 2 | **Difficulty filter**: `difficultyLevel` (multi-select). | pending |
| 3 | **Duration range**: `durationMin` / `durationMax`. | pending |
| 4 | **Tags filter**: multi-value with ANY/ALL semantics. A single-value `tag` param already exists (Explore tag cloud, HQL `MEMBER OF`). | partial |
| 5 | **Composer sort**: add to `ALLOWED_SORT_FIELDS` in `SheetRepository` (needs a join, since composer is a musician reference). | pending |
| 6 | **"Has issues" flag**: a sheet with any instrumentation in `DAMAGED`/`LOST` condition, or `INCOMPLETE` coverage. | pending |
| 7 | **Bulk add to setlist**: `POST /collections/{id}/items/bulk` with a list of sheet IDs. | pending |
| 8 | **Bulk archive**: `POST /sheets/bulk-archive` (needs an archive flag or status, which doesn't exist yet; decide first). | pending |
| 9 | **Bulk export**: `POST /sheets/bulk-export`, a ZIP via the existing `SheetExportService` / `ExportResult` pattern. | pending |
| 10 | **Angular toolbar**: wire the new filters (URL-synced) and a selection-based bulk action bar in `sheets.ts`. | pending |

## Key files

- `api/src/main/java/de/halbmann/sam/api/entity/sheets/SheetFilterRequest.java`
- `server/src/main/java/de/halbmann/sam/business/sheets/boundary/SheetRepository.java`
- `server/src/main/java/de/halbmann/sam/business/sheets/controller/SheetService.java`
- `ui/src/main/webui/src/app/features/sheets/` (`sheets.ts`, `sheets.html`)
