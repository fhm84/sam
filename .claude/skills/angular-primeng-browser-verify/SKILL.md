---
name: angular-primeng-browser-verify
description: Strict browser verification for Angular apps using PrimeNG. Use when a rendered Angular UI must be validated in a real browser with Playwright MCP, especially for dialogs, overlays, tables, forms, routing, responsiveness, and browser-visible regressions.
---

# Angular PrimeNG Browser Verify

Use this skill for Angular applications that use PrimeNG components.

This skill is stricter than a generic browser verification workflow. It assumes the UI may look correct in code while still failing in the browser because of PrimeNG overlays, async rendering, routing state, CSS layering, change detection timing, or viewport-specific issues.

## Rule of engagement

Always use Playwright MCP for browser verification when available.

Do not treat:
- successful compilation,
- passing unit tests,
- clean TypeScript,
- or visually plausible templates

as proof that the rendered Angular UI is correct.

Browser verification is the source of truth for this skill.

## Preconditions

Before starting:
- Confirm the target URL.
- Confirm the Angular app is running.
- If multiple app shells or routes exist, identify the exact route to verify.
- If Playwright MCP is unavailable, stop and explain that strict browser verification cannot be completed.

## Required verification sequence

Follow this order unless the user asked for a smaller scope:

1. Open the target route with Playwright MCP.
2. Wait for the application shell to stabilize.
3. Verify that the page is not stuck in a loading, blank, or partially bootstrapped state.
4. Check browser console for:
    - uncaught errors,
    - Angular runtime errors,
    - template/rendering errors,
    - PrimeNG-related warnings,
    - missing asset or API errors.
5. Check network activity for:
    - failed API calls,
    - blocked assets,
    - requests that leave the page unusable.
6. Inspect visible UI structure.
7. Exercise the main user flow.
8. Re-check console and network after interaction.
9. Repeat critical checks on desktop and mobile widths.
10. Report issues with exact browser evidence.

## Angular-specific checks

Always check these when relevant:
- The router outlet rendered the expected page.
- Navigation updates the visible page state correctly.
- Route guards or redirects do not silently break navigation.
- Async data sections settle correctly after loading.
- No obvious flicker, hydration-like mismatch, or stale content remains visible after interaction.
- Buttons, tabs, accordions, and dynamic sections actually update the DOM as expected.
- Validation messages appear only when appropriate and disappear when corrected.
- Disabled states, loading states, and empty states are visible and coherent.

## PrimeNG-specific checks

PrimeNG components often fail in browser-visible ways even when code seems correct.

Always check these when relevant:
- Dialogs open, close, and trap focus correctly.
- Dropdowns, autocomplete panels, overlay panels, menus, confirm popups, calendars, and multiselect panels render above surrounding content.
- No overlay is clipped by parent overflow.
- No overlay appears behind headers, drawers, or dialogs.
- Overlay positioning is visually correct after scroll and resize.
- Escape closes dismissible overlays where expected.
- Clicking outside dismisses overlays where expected.
- Scrollable tables, virtual scroll lists, and lazy-loaded data sections remain usable.
- Paginators, sort headers, row expansion, filters, and selection controls work visibly.
- Toasts, inline messages, and confirm dialogs appear in the right visual layer and do not block unrelated interaction.
- Focus return after closing dialog/overlay is reasonable.
- No duplicated mask, stuck overlay, or frozen page remains after close.

## Responsive checks

Test at minimum:
- Desktop: 1440x900
- Mobile: 375x812

At both sizes, verify:
- No horizontal overflow unless explicitly intended.
- PrimeNG tables and forms remain usable.
- Dialogs and overlays remain reachable and not clipped off-screen.
- Sticky headers, toolbars, and action bars do not cover critical controls.
- Primary actions remain visible and operable.
- Touch-sized targets are usable on mobile.

## Severity policy

Classify issues using this rule:

- critical: main workflow blocked, blank page, broken navigation, fatal overlay issue, impossible form submission, unusable table
- major: confusing or broken interaction, hidden controls, clipped overlay, missing validation, important console/API failure, mobile layout break
- minor: visual misalignment, spacing issue, low-impact overflow, non-blocking warning, cosmetic inconsistency

## Output format

Use this exact structure:

### Result
Pass, Partial, or Fail.

### Scope
- URL:
- Route:
- Viewports:
- Main flow tested:

### Browser evidence
- Console:
- Network:
- Visible UI observations:

### Issues
For each issue:
- Severity:
- Component or area:
- Reproduction steps:
- Observed result:
- Expected result:
- Likely cause:
- Recommended fix:

### Verification status
- What passed:
- What still failed:
- What was not checked:

## Interaction discipline

When interacting:
- Prefer user-facing locators and visible labels over brittle internal selectors, consistent with Playwright best practices.
- Follow the primary user journey first.
- For forms, test both invalid and valid paths.
- For tables, test at least one sort, one filter, one pagination or scroll interaction if present.
- For overlays, explicitly open and close them and observe the post-close page state.

## Examples of high-value prompts

- Use Playwright MCP to verify the PrimeNG dialog workflow on /customers and check whether the dialog, confirm popup, and toast stack correctly.
- Use Playwright MCP to test the editable PrimeNG table on /contracts and report filtering, row expansion, paginator, and mobile overflow issues.
- Use Playwright MCP to verify the form on /settings, including invalid input, valid submission, and post-submit toast behavior.

## Boundaries

Do not:
- Claim success based only on Angular code inspection.
- Ignore console errors because the page "looks okay".
- Skip overlay testing when PrimeNG components are involved.
- Stop after desktop-only verification when responsive layout is relevant.
- Invent evidence that was not observed in the browser.

## Completion rule

The task is complete only when you have either:
- verified the requested Angular/PrimeNG behavior in a real browser with Playwright MCP, or
- clearly stated why that verification could not be performed.