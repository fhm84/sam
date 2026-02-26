## Architecture Guidelines

This Angular application follows a feature-based, component-driven architecture.

### Key Rules
- Organize code by feature
- Use standalone components
- Lazy-load all features via `loadChildren` pointing to per-feature `<feature>.routes.ts` files
- Keep components thin
- Move logic to services or stores
- Use smart/container and dumb/presentational components
- Smart sub-components are permitted for self-contained CRUD panels that are too complex to be
  presentational but do not map to a route (e.g. an embedded list with add/edit/remove dialogs).
  They MAY inject services and own state, but MUST stay within their feature folder and MUST
  receive their scope (e.g. parent entity id) via @Input.

### Structure
- `/core`: app-wide services (auth, api, guards)
- `/shared`: reusable UI, pipes, directives (no business logic)
- `/features`: isolated, lazy-loaded feature domains

### State
- Prefer local state
- Avoid global state unless necessary

### Quality
- One responsibility per file
- No cross-feature imports
- Explicit naming conventions
