# Plan: Home Dashboard

Roadmap: [Home dashboard](../roadmap.md#home-dashboard--planned)

**Why:** after login the app lands on an empty screen. The design (`Hi-Fi Shell`) shows
an Inbox tab (KPIs, activity feed) and an Ensemble dashboard tab (coverage KPIs, gap cards,
next concert).

## Decisions

- **Collection venue**: a plain nullable `venue` string on `SheetCollectionEntity`.

## Open questions

- "3 new sheets from CLI awaiting review" implies a review workflow (sheet review status).
  Scope is undefined; leave it out of the first version.

## Data model

| # | Task | Status |
|---|------|--------|
| 1 | `venue` on `SheetCollectionEntity` / `SheetCollection` + migration. | pending |
| 2 | `MEMBER_JOINED` / `MEMBER_LEFT` in `EventType`, emitted from `EnsembleMembershipService`. | pending |
| 3 | `sam.coverage.stale-threshold-days` (default 7) + staleness flag in the coverage status response. Mostly moot once [snapshot invalidation](coverage-snapshot-invalidation.md) ships; reconsider then. | pending |

## API

| # | Task | Status |
|---|------|--------|
| 4 | `GET /home/inbox-stats`: to-classify count, instrumentations without archive location, stale ensembles, new sheets this week. | pending |
| 5 | `GET /home/activity`: recent event-log entries with display labels (respect the [role-aware access](rbac-role-aware-access.md) rules). | pending |
| 6 | `GET /home/next-concert`: nearest upcoming `SETLIST` with venue, piece count, total duration, aggregate coverage. | pending |
| 7 | `GET /ensembles/{id}/dashboard`: complete / playable / incomplete counts + repertoire size. | pending |
| 8 | `GET /ensembles/{id}/gaps`: shared with [coverage breakdown](coverage-breakdown.md), task 3. | pending |
| 9 | `GET /ensembles/{id}/near-complete`: sheets within N% of the PLAYABLE threshold. | pending |

## Angular

| # | Task | Status |
|---|------|--------|
| 10 | `/home` route as the default landing page, with Inbox and Ensemble dashboard tabs. | pending |
| 11 | Inbox tab: KPI row, activity feed, right rail (quick upload, gap card, next concert). | pending |
| 12 | Ensemble tab: KPI row, most-missing voices, one-voice-away list. | pending |
| 13 | Venue field in the collection form. | pending |

## Key files

- `api/src/main/java/de/halbmann/sam/api/entity/collections/SheetCollection.java`
- `server/src/main/java/de/halbmann/sam/business/collections/entity/SheetCollectionEntity.java`
- `api/src/main/java/de/halbmann/sam/api/entity/eventlog/EventType.java`
- `server/src/main/java/de/halbmann/sam/business/ensembles/controller/` (`EnsembleMembershipService`, `CoverageSnapshotService`)
- `ui/src/main/webui/src/app/app.routes.ts`, new `features/home/`
