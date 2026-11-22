# Band Branding

An installation can show the band's **name** and **logo** next to SAM's own logo. Without
configuration nothing changes; no logo is part of the repository or the images.

## Where it shows

| Place | What |
|---|---|
| Top bar | Logo and name after the SAM logo, separated by a line; on narrow screens the logo alone |
| Public share page (`/share/{token}`) | Name and logo in the header, so guests see whose sheets they were sent |
| Collection TOC PDF | Name above the title, logo at the top right (scaled into 5 cm × 1.6 cm) |

The login page belongs to the OIDC provider; Pocket ID has its own logo setting.

## Configuration

| Property | Env var | Meaning |
|---|---|---|
| `sam.branding.name` | `SAM_BRANDING_NAME` | The band's name |
| `sam.branding.logo` | `SAM_BRANDING_LOGO` | Path to the logo file: SVG, PNG, JPEG, GIF or WebP, at most 1 MiB |
| `sam.branding.print-logo` | `SAM_BRANDING_PRINT_LOGO` | PNG, JPEG or GIF for PDFs; defaults to the logo when that is one of these |

The PDF renderer (Flying Saucer) can't draw SVG: with an SVG logo, set a PNG as
`print-logo`, or the TOC goes without a logo. The type is detected from the file's content,
not its name. Files are read once at startup; after replacing one, restart `sam-server`. A
file that can't be used (missing, too large, not an image) is left out with a warning in the
log, and SAM starts anyway.

In production, put the files into `branding/` next to `docker-compose.prod.yml` (mounted
read-only as `/branding`, see [production setup](../architecture/production-setup.md#3-deploy-the-stack))
and set, for example, `SAM_BRANDING_LOGO=/branding/logo.svg` in `.env`.

## How it works

`BrandingService` (`server/.../branding/`) loads the files at startup; `BrandingResource`
serves them at `GET /api/public/branding` (`{ name, logoVersion }`) and
`GET /api/public/branding/logo?v=<logoVersion>`. Both are public: the share page needs them
without a login. `logoVersion` is a hash of the file, so the browser may cache the logo for
good (`immutable`) and still gets a new one after a change; the logo response carries a
restrictive `Content-Security-Policy`, so an SVG opened directly can't run scripts. The UI
reads both once (`BrandingService` in `ui/.../core/`). The TOC gets the print logo as a data
URI, which the renderer decodes without fetching anything.

## Related

- [Roadmap: band branding](../roadmap.md#band-branding-logo-and-name--in-progress) — what's next (admin upload, per-ensemble logo)
- [Roadmap: watermarking](../roadmap.md#watermarking-on-shared--downloaded-documents--idea) — the logo on shared PDFs
- [Shares & Public Access](shares.md)
- [Collections & Setlists](collections.md) — the TOC export
