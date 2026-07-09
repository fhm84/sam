# UI & Personalisation

## Layout modes

The Sakai-based layout supports three sidebar modes (persisted in `localStorage`):

| Mode | Description |
|------|-------------|
| Static | Sidebar always visible |
| Overlay | Sidebar overlays content |
| Slim | Icon-only sidebar |

## Theme

- **Light** and **dark** mode, toggleable from the topbar.
- Colours driven by CSS custom properties (PrimeNG Aura preset).

## Language

- **English** (default) and **German** — selectable from the topbar.
- Translations loaded from `public/i18n/{en,de}.json`.
- All UI strings, enum labels, and messages are fully translated in both languages.

## User preferences page

`/user/preferences` — configure layout, theme, and language. Settings persisted in `localStorage`.

## Related

- [My Parts](my-parts.md) — the personalised content view
