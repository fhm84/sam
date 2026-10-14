# Plan: Sheets Overview — Filters, Sort & Bulk Actions

Roadmap: [Sheets overview filter & bulk actions](../roadmap.md#sheets-overview-filter--bulk-actions--planned)
· Tracking issue: [#182](https://github.com/fhm84/sam/issues/182) (milestone [Everyday use](https://github.com/fhm84/sam/milestone/2))
· Feature: [Search & discovery](../features/search-discovery.md)

**Why:** the design's filter toolbar and bulk actions go beyond what `SheetFilterRequest`
and the API support today. Related: the [instrumentation-count filter](../features/search-discovery.md#instrumentation-count-filter)
("exactly 4 horns, no oboes") already goes through the same filter request.

## Tasks

| # | Task | Status |
|---|------|--------|
| 1 | **Coverage filter**: `coverageStatus` (list) + `ensembleId` on `SheetFilterRequest`, joined via coverage snapshots. Only meaningful once [snapshot invalidation](coverage-snapshot-invalidation.md) keeps them fresh. | [#183](https://github.com/fhm84/sam/issues/183) |
| 2 | **Difficulty filter**: `difficultyLevel` (multi-select). | [#184](https://github.com/fhm84/sam/issues/184) |
| 3 | **Duration range**: `durationMin` / `durationMax`. | [#185](https://github.com/fhm84/sam/issues/185) |
| 4 | **Tags filter**: multi-value with ANY/ALL semantics. A single-value `tag` param already exists (Explore tag cloud, HQL `MEMBER OF`). | [#186](https://github.com/fhm84/sam/issues/186) |
| 5 | **Composer sort**: add to `ALLOWED_SORT_FIELDS` in `SheetRepository` (needs a join, since composer is a musician reference). | [#187](https://github.com/fhm84/sam/issues/187) |
| 6 | **"Has issues" flag**: a sheet with any instrumentation in `DAMAGED`/`LOST` condition, or `INCOMPLETE` coverage. | [#188](https://github.com/fhm84/sam/issues/188) |
| 7 | **Bulk add to setlist**: `POST /collections/{id}/items/bulk` with a list of sheet IDs. | [#189](https://github.com/fhm84/sam/issues/189) |
| 8 | **Bulk archive**: `POST /sheets/bulk-archive` (needs an archive flag or status, which doesn't exist yet; decide first). | [#190](https://github.com/fhm84/sam/issues/190) |
| 9 | **Bulk export**: `POST /sheets/bulk-export`, a ZIP via the existing `SheetExportService` / `ExportResult` pattern. | [#191](https://github.com/fhm84/sam/issues/191) |
| 10 | **Angular toolbar**: wire the new filters (URL-synced) and a selection-based bulk action bar in `sheets.ts`. | [#192](https://github.com/fhm84/sam/issues/192) |

## Key files

- `api/src/main/java/de/halbmann/sam/api/entity/sheets/SheetFilterRequest.java`
- `server/src/main/java/de/halbmann/sam/business/sheets/boundary/SheetRepository.java`
- `server/src/main/java/de/halbmann/sam/business/sheets/controller/SheetService.java`
- `ui/src/main/webui/src/app/features/sheets/` (`sheets.ts`, `sheets.html`)
