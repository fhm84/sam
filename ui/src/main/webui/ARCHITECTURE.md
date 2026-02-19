# UI Module Architecture

Angular 21 + PrimeNG 21 frontend for the SAM (Sheet Archive for Music) application.

## Tech Stack

| Concern        | Choice                                  |
| -------------- | --------------------------------------- |
| Framework      | Angular 21 (standalone components)      |
| UI Library     | PrimeNG 21 with Aura theme              |
| Styling        | SCSS + CSS custom properties            |
| Build          | Angular CLI / `@angular/build`          |
| Test           | Vitest                                  |
| Serving        | Quarkus Quinoa (dev & prod)             |
| API Proxy      | `proxy.conf.json` -> `localhost:8080`   |

## Directory Structure

```
src/app/
├── core/                        # Singleton services (provided in root)
│   ├── api/                     # Pure HTTP wrappers — 1:1 with backend resources
│   │   ├── index.ts             # Barrel export
│   │   ├── sheets-api.service.ts
│   │   ├── instrumentations-api.service.ts
│   │   ├── documents-api.service.ts
│   │   ├── musicians-api.service.ts
│   │   ├── instruments-api.service.ts
│   │   ├── ensembles-api.service.ts
│   │   ├── collections-api.service.ts
│   │   └── booklets-api.service.ts
│   ├── theme.service.ts         # Dark mode toggle + persistence
│   └── translation.service.ts   # Runtime i18n (EN/DE)
│
├── shared/                      # Reusable UI components, pipes, utils
│   ├── pipes/
│   │   └── translate.pipe.ts    # {{ 'key' | translate }} pipe
│   └── utils/
│       └── object.utils.ts      # Pure helper functions
│
├── model/                       # TypeScript types
│   └── datamodels.d.ts          # Auto-generated from API module (do not edit)
│
├── features/                    # Feature modules — self-contained pages
│   ├── sheets/
│   ├── collections/
│   ├── uploads/
│   ├── musicians/
│   ├── ensembles/
│   ├── instruments/
│   └── configuration/
│
├── app.ts                       # Root component (shell: topbar + sidebar)
├── app.html                     # Root template
├── app.scss                     # Root styles
├── app.routes.ts                # Top-level routes with lazy loading
└── app.config.ts                # Application providers
```

## Layer Rules

### `core/api/` — API Services

Thin HTTP wrappers. Each service maps 1:1 to a backend JAX-RS resource.

**Rules:**
- Only inject `HttpClient`
- Return `Observable<T>` with proper types from `model/datamodels`
- No caching, no transforms, no side effects
- Method naming: `find`, `load`, `create`, `update`, `delete`

**Import via barrel:**
```typescript
import { SheetsApiService } from '../../core/api';
```

### `core/` — Application Services

Singleton services that don't belong to any feature: theme management, future
auth guards/interceptors, global error handling.

### `shared/` — Reusable Pieces

Components, directives, pipes, and utility functions used across multiple features.

**Rules:**
- No dependencies on `features/` or `core/api/`
- Utility functions in `shared/utils/` must be pure (no Angular DI)

### `model/` — Types

- `datamodels.d.ts` is **auto-generated** by the `api` Maven module via
  `typescript-generator-maven-plugin`. Do not edit manually.
- Add manual type extensions as separate `.ts` files if needed.

### `features/` — Feature Modules

Each directory is a self-contained feature with its own components, routes,
and optionally feature-specific services.

**Rules:**
- Can import from `core/`, `shared/`, `model/`
- **Never** import from another feature
- Use lazy loading via `loadComponent` in routes

**Internal structure (when a feature grows):**
```
features/sheets/
├── sheets.ts                  # List/overview component (entry point)
├── sheets.routes.ts           # Child routes (optional)
├── sheet-detail/              # Sub-component
│   ├── sheet-detail.ts
│   ├── sheet-detail.html
│   └── sheet-detail.scss
└── services/                  # Feature-specific state/logic (optional)
    └── sheets-state.service.ts
```

## API Surface

The `core/api/` services cover the full backend API:

| Service                     | Base Path                                              | Resources         |
| --------------------------- | ------------------------------------------------------ | ----------------- |
| `SheetsApiService`          | `/api/sheets`                                          | CRUD + coverage   |
| `InstrumentationsApiService`| `/api/sheets/{sheetId}/instrumentations`               | CRUD + bulk       |
| `DocumentsApiService`       | `/api/documents`, also sub-resource of sheets/instrum. | list, download, upload |
| `MusiciansApiService`       | `/api/musicians`                                       | CRUD              |
| `InstrumentsApiService`     | `/api/instruments`                                     | CRUD              |
| `EnsemblesApiService`       | `/api/ensembles`                                       | CRUD + voices + voice options |
| `CollectionsApiService`     | `/api/sheet-collections`                               | CRUD + sheets     |
| `BookletsApiService`        | `/api/booklets`                                        | CRUD + sheets     |

`DocumentsApiService` is parameterized by base path since the backend mounts
it at three different locations (top-level, under sheets, under instrumentations).

## Theming

- PrimeNG Aura theme with `darkModeSelector: '.dark'`
- Custom design tokens via `--sam-*` CSS variables in `styles.scss`
- Dark mode toggled by `ThemeService` (adds/removes `.dark` on `<html>`)
- Persisted to `localStorage`, defaults to system preference

## Internationalization (i18n)

- **Runtime i18n** via `TranslationService` in `core/` — no build-time localization
- Translation files: `public/i18n/en.json` and `public/i18n/de.json`
- Use dotted key paths: `"nav.sheets"`, `"nav.admin.ensembles"`, etc.
- In templates: `{{ 'key.path' | translate }}` or `[attr.aria-label]="'key' | translate"`
- In TypeScript: `inject(TranslationService).t('key.path')`
- PrimeNG component labels are auto-configured from the `primeng` section of each locale file
- Locale is persisted to `localStorage` (`sam-locale`) and defaults to browser language
- To add a new locale: create `public/i18n/<locale>.json` and add the code to `SUPPORTED_LOCALES` in `translation.service.ts`

## Conventions

- **File naming:** Angular 21 convention — `sheets.ts` not `sheets.component.ts`
- **Components:** Standalone (no NgModules)
- **Routing:** Lazy-loaded via `loadComponent` in `app.routes.ts`
- **Styles:** Component SCSS + global `styles.scss` for tokens and resets
- **Types:** Always use generated types from `model/datamodels` — no `any`
