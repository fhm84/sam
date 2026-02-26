# Angular Application Architecture (Canonical)

This document defines the authoritative architecture rules for this Angular codebase.
All human- and AI-generated code MUST comply with this document.

----------------------------------------------------------------

## 1. Architectural Principles

- Component-based composition
- Feature-based organization
- Standalone components by default
- Lazy-loaded features
- Thin components, thick services
- Unidirectional data flow
- Explicit boundaries between features

----------------------------------------------------------------

## 2. Folder Structure

    /app
      app.component.ts
      app.routes.ts

      /core
        api.service.ts
        auth.service.ts
        interceptors/
        guards/

      /shared
        /ui            # presentational components only
        /pipes
        /directives
        /utils

      /features
        /<feature>
          <feature>.routes.ts      # route configuration
          <feature>.page.ts        # route entry (smart component)
          <feature>.service.ts
          <feature>.store.ts       # optional
          /components              # dumb UI components
          # smart sub-components live at feature root alongside pages

### Structural Rules

- Organize code by feature, never by file type
- Each feature is self-contained
- Features MUST NOT import from other features
- Features MAY import from shared and core
- Shared and core MUST NOT import from features

----------------------------------------------------------------

## 3. Components

### Smart (Container) Components

Smart components are responsible for orchestration.

Responsibilities:
- Own state and side effects
- Fetch data via services or stores
- Handle routing concerns
- Coordinate child components

Rules:
- MAY inject services or stores
- MAY manage signals or observables
- SHOULD remain thin
- SHOULD NOT contain UI-heavy logic

---

### Dumb (Presentational) Components

Dumb components are UI-only.

Responsibilities:
- Render data passed via @Input
- Emit events via @Output
- Contain view and presentation logic only

Rules:
- MUST NOT inject services
- MUST NOT perform side effects
- MUST NOT own application state
- SHOULD be reusable and stateless

---

### Smart Sub-Components

Smart sub-components are an explicitly permitted pattern for self-contained feature panels
that are too complex to be presentational but do not map to a route of their own.

Examples: a CRUD panel embedded in a detail page (e.g. `CollectionSheets`, `VoiceOptions`).

Responsibilities:
- Own their local state and lifecycle
- Communicate with services directly
- Coordinate child components within their scope

Rules:
- MAY inject services
- MAY own signals and perform side effects
- MUST NOT be shared across features
- MUST live inside the feature folder (not in `/shared`)
- Input to scope them (e.g. a parent entity id) MUST be passed via @Input

----------------------------------------------------------------

## 4. Routing

- Routing is defined per feature
- All features MUST be lazy-loaded
- Each feature exposes its own route configuration in a `<feature>.routes.ts` file

Example:

    // features/sheets/sheets.routes.ts
    export const SHEETS_ROUTES: Routes = [
      { path: '', component: Sheets },
      { path: ':id', component: SheetDetailPage },
    ];

    // app.routes.ts
    {
      path: 'sheets',
      loadChildren: () => import('./features/sheets/sheets.routes').then((m) => m.SHEETS_ROUTES),
    }

Rules:
- app.routes.ts only wires lazy-loaded features via `loadChildren`
- No feature routes are defined at the app level
- Each feature route file uses direct component references (not nested `loadComponent`)
- The exported constant is named `<FEATURE>_ROUTES` (screaming snake case)

----------------------------------------------------------------

## 5. State Management

### State Placement Rules

- Component-local state → component signals
- Feature-level state → feature store or service
- Global state → ONLY when explicitly required

Guidelines:
- Prefer local state
- Keep state close to where it is used
- Avoid global state by default
- Do not introduce state libraries prematurely

----------------------------------------------------------------

## 6. Services

Responsibilities:
- Encapsulate business logic
- Handle API communication
- Perform side effects

Rules:
- No UI logic
- Scoped to a feature or core
- One service per domain concern
- Components delegate logic to services

----------------------------------------------------------------

## 7. Shared Code Rules

Shared contains reusable UI and utilities.

Rules:
- Shared MUST NOT depend on features
- Shared MUST NOT contain business logic

### Shared UI Components

Rules:
- Presentational only
- Inputs and Outputs only
- No services
- No side effects

----------------------------------------------------------------

## 8. Core Layer Rules

Core contains application-wide services.

Includes:
- Authentication
- API clients
- Guards
- Interceptors

Rules:
- Loaded once at application startup
- MUST NOT depend on features
- MUST NOT contain feature-specific logic

----------------------------------------------------------------

## 9. Code Quality Rules

- Components SHOULD remain under ~200 lines
- One responsibility per file
- Explicit naming only
- Avoid abbreviations
- Prefer clarity over cleverness

----------------------------------------------------------------

## 10. Import Rules

- No cross-feature imports
- Features MAY import from shared and core
- Shared MUST NOT import from features
- Core MUST NOT import from features

----------------------------------------------------------------

## 11. AI Coding Rules (Mandatory)

When generating Angular code:

- Follow this architecture exactly
- Use standalone components
- Organize code by feature
- Lazy-load all features
- Keep components thin
- Move logic to services or stores
- Do not introduce global state unless explicitly requested
- Do not invent new architectural patterns

----------------------------------------------------------------

## 12. AI System Prompt (Ultra-Short)

    You are generating Angular code.

    Follow feature-based architecture.
    Use standalone components only.
    Lazy-load all features.
    Keep components thin.
    Move logic to services or stores.
    Use smart/container and dumb/presentational components.
    No cross-feature imports.
    Shared code has no business logic.
    Do not introduce global state unless requested.
    Follow the predefined folder structure exactly.

----------------------------------------------------------------

## 13. AI Validation Checklist

Architecture:
- Feature-based folders only
- No type-based root folders
- Lazy-loaded feature routes

Components:
- Standalone components
- Smart vs Dumb separation (smart sub-components are permitted — see §3)
- No services in dumb components
- Smart sub-components scoped to a single feature, receive parent id via @Input
- Components under ~200 lines

State & Logic:
- Business logic in services or stores
- No unnecessary global state
- Unidirectional data flow

Shared:
- Shared has no feature dependencies
- Shared UI components are presentational

Imports:
- No cross-feature imports
- Explicit, clear import paths

----------------------------------------------------------------

## 14. AI Guardrail Prompt (Strict Mode)

    Do not invent new architectural patterns.
    Do not introduce NgModules unless required.
    Do not introduce global state.
    Do not merge features.
    Do not place business logic in components.
    Follow the architecture rules verbatim.

----------------------------------------------------------------

## 15. Enforcement

This document is the single source of truth for architecture decisions.

Any deviation must be:
- Explicitly justified
- Documented
- Approved

Failure to comply is considered an architectural defect.
