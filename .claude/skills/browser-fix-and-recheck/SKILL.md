---
name: browser-fix-and-recheck
description: Fix browser-visible UI issues and re-verify them in a real browser with Playwright MCP. Use when the task is not only to inspect, but to implement a fix, rerun the app, and confirm the result with browser evidence.
---

# Browser Fix And Recheck

Use this skill when browser-visible issues must be fixed and then verified again in a real browser.

This skill is execution-oriented. It does not stop at identifying issues. It requires a fix, a rerun, and a browser-based confirmation.

## Core rule

Never claim a UI issue is fixed until the rendered result has been rechecked in Playwright MCP.

Code changes alone are not enough.

## Default loop

Follow this sequence:

1. Reproduce the issue in Playwright MCP.
2. Capture exact evidence:
    - visible browser behavior,
    - console errors,
    - failed requests,
    - route/state details,
    - screenshot if useful.
3. Identify the smallest high-confidence fix.
4. Edit the code.
5. Ensure the app rebuilds or refreshes.
6. Re-open or refresh the affected route.
7. Reproduce the same steps again.
8. Confirm whether:
    - the original issue is resolved,
    - no obvious regression was introduced,
    - console/network state is still acceptable.
9. Report the result clearly.

## Prioritization

Fix in this order:
- broken user flow,
- broken overlay or blocked interaction,
- data loss or impossible submission,
- severe layout break,
- important validation problem,
- cosmetic issue

Fix the highest-impact issue first unless the user explicitly prioritizes something else.

## Recheck requirements

After each fix, recheck:
- the original reproduction path,
- one nearby interaction likely to regress,
- console errors,
- failed network requests,
- desktop and mobile if layout or overlays are involved.

## Output format

Use this exact structure:

### Issue addressed
- Area:
- Original problem:
- Severity:

### Change made
- Files changed:
- What was changed:
- Why this fix was chosen:

### Recheck
- URL or route:
- Steps repeated:
- Browser result after fix:
- Console status:
- Network status:
- Regression check:

### Status
Resolved, Partially resolved, Not resolved.

### Remaining concerns
- Any follow-up issue
- Any uncertainty
- Any additional test worth running

## Angular and PrimeNG guidance

When working on Angular or PrimeNG UIs:
- Recheck overlays after any CSS, container, or z-index change.
- Recheck change-detection-sensitive UI after event handling or async logic changes.
- Recheck tables, paginator state, dialogs, and form validation if the fix touched shared components.
- If a fix changes layout containers, test both desktop and mobile again.

## Prompting discipline

Explicitly say "Use Playwright MCP" when reproducing and when rechecking.

Examples:
- Use Playwright MCP to reproduce the clipped PrimeNG dropdown, fix the layout or overlay issue, then recheck on desktop and mobile.
- Use Playwright MCP to reproduce the broken validation message flow, implement a fix, and confirm the rendered result after submission.

## Boundaries

Do not:
- mark an issue resolved without rerunning the browser flow,
- silently change unrelated behavior without checking regressions,
- stop after the first code change if the browser still shows a failure,
- describe assumed results instead of observed results.

## Completion rule

A task is complete only when one of these is true:
- the issue was reproduced, fixed, and verified in Playwright MCP,
- the issue was reproduced and a fix was attempted but failed, with evidence,
- or browser re-verification was impossible and that limitation was stated explicitly.