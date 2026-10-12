# Plan: Tailwind CSS v4 Upgrade

Roadmap: [Tailwind CSS v4 upgrade](../roadmap.md#tailwind-css-v4-upgrade--planned)
· Stack: [tech stack](../architecture/tech-stack.md)

**Why:** Tailwind 3.4 pulls in build-time dependencies with open advisories
(`braces`, `micromatch`, `postcss-selector-parser`), and the fixes ship only with v4.
Dependabot PR #120 tried the bump on its own and broke the UI build. This plan does the
upgrade as one reviewed change instead. There is no urgency: the advisories only affect
the CSS build, which processes our own files.

## Findings from a trial migration (2026-10-12)

A throwaway run of `@tailwindcss/upgrade@4.3.3` on `main` showed:

1. **The upgrade tool's output builds but drops most utilities.** It replaces
   `@tailwind utilities` with `@import 'tailwindcss/utilities' layer(utilities)` only.
   Without the theme, v4 generates no utilities that need a theme value (spacing, font
   sizes, font weights): `gap-2` (122 uses) had no rule in the output, and the build
   still reported success. Importing `tailwindcss/theme.css` as well fixes it.
2. **`postcss.config.mjs` is dead config.** Angular only reads `postcss.config.json` or
   `.postcssrc.json` (`@angular/build`, `postcss-configuration.js`). Without one it falls
   back to `tailwind.config.js` and loads `tailwindcss` as a PostCSS plugin itself, which
   v4 refuses (the #120 CI failure). The `autoprefixer` entry in it was never applied.
3. **`max-[960px]:grid-cols-1` and `min-[900px]:inline` don't work today.** The object
   screen `xs: { max: '540px' }` turns off `min-*`/`max-*` variants in v3 (the build
   warning about `screens` objects). v4 generates them, so `home.html`, `sheets.html` and
   `sheet-form-page.html` will change at 900–960 px widths. That is intended, but check it.
4. **Cascade layers are the main visual risk.** Unlayered CSS always beats layered CSS.
   Optimus UI's runtime theme (`provideOptimus` without `cssLayer`), `styles.scss` and
   component styles are all unlayered, so utilities in `@layer utilities` would lose to
   all of them. `tailwindcss/utilities.css` is just `@tailwind utilities;`, so importing
   it **without** `layer()` keeps v3's cascade.
5. **Few class changes matter.** Required: `!text-center` → `text-center!` (8),
   `focus:outline-none` → `focus:outline-hidden` (1). Recommended:
   `[grid-template-columns:…]` → `grid-cols-[…]` (~57). Optional: ~400 lines of
   `text-[var(--x)]` → `text-(--x)` (v4 still accepts the old form). The templates use no
   default-palette colours (everything goes through `--sam-*` / `--p-*`), and none of the
   renamed `shadow` / `rounded` / `blur` scales.

## Decisions

- Utilities stay unlayered (finding 4). Only revisit with `cssLayer` on `provideOptimus`
  if Optimus overrides need to lose to utilities, which would be a behaviour change.
- Preflight stays off, as today.
- The `xs` screen moves to CSS and `tailwind.config.js` is deleted (no `@config` bridge;
  that bridge also emits a stray `.container` breakpoint rule).

## Tasks

| # | Task | Status |
|---|------|--------|
| 1 | Dependencies: `tailwindcss@^4.3.3`, add `@tailwindcss/postcss`, remove the unused `autoprefixer`. | pending |
| 2 | Replace `postcss.config.mjs` with `.postcssrc.json` (`{"plugins": {"@tailwindcss/postcss": {}}}`). | pending |
| 3 | Rewrite `src/tailwind.css`: `@import 'tailwindcss/theme.css' layer(theme);`, `@import 'tailwindcss/utilities.css';` (no layer), `@custom-variant xs (@media (width <= 540px));`. Delete `tailwind.config.js`. | pending |
| 4 | Class rewrites: required + recommended ones (finding 5) in one commit, the optional `(--x)` rewrite in a separate commit so it can be reviewed or dropped on its own. | pending |
| 5 | Build guard: a check after `ng build` (CI and locally) that fails if sentinel utilities (`gap-2`, `text-sm`, `p-4`) are missing from the output CSS, so a silent drop like finding 1 can't recur. | pending |
| 6 | Verify: `tsc --noEmit`, `ng test`, `ng build`, then before/after screenshots (Playwright, `target/playwright/`) of home, sheets list, sheet detail, sheet form, collections, upload classification dialog and public share page. Check desktop, ~950 px (finding 3), ≤540 px (`xs`) and dark mode, plus hover on Optimus buttons and inputs. | pending |
| 7 | Browser support: v4 needs Safari 16.4+, Chrome 111+, Firefox 128+. Confirm the band's devices (older iPads in particular) meet it before merging. | pending |
| 8 | Housekeeping: update the Tailwind row in `docs/architecture/tech-stack.md`, keep the Tailwind-major ignore in `.github/dependabot.yml` (for v5), check that `npm audit` no longer reports the Tailwind 3 advisories. | pending |

## Key files

- `ui/src/main/webui/package.json`, `package-lock.json`
- `ui/src/main/webui/postcss.config.mjs` (to delete), `.postcssrc.json` (new), `tailwind.config.js` (to delete)
- `ui/src/main/webui/src/tailwind.css`, `angular.json` (`styles`: `tailwind.css` before `styles.scss`)
- `ui/src/main/webui/src/app/app.config.ts` (`provideOptimus`)
- `.github/workflows/ci.yml` (UI job, for the build guard)
