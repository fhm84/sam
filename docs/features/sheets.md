# Sheet Music

The central entity. Each sheet represents one piece of music in the archive.

## Metadata fields

| Field | Type | Notes |
|-------|------|-------|
| Title | String (required) | |
| Subtitle | String | |
| Composer | Musician reference | Linked to the [musicians](musicians.md) catalogue |
| Arranger | Musician reference | Linked to the [musicians](musicians.md) catalogue |
| Original by | String | Original band / artist for covers/arrangements |
| Genre | Enum (required) | Structural form: March, Suite, Overture, etc. — see [Genre enum](#genre) |
| Style | Enum | Stylistic character: Classical, Swing, Pop, etc. — see [Style enum](#style) |
| Difficulty level | 1–6 scale | Wind-band grading: 1 = very easy … 6 = very difficult |
| Duration | hh:mm:ss | Approximate playtime |
| Tempo | Integer (bpm) | |
| Tonality | Enum | Musical key — see [Tonality enum](#tonality) |
| Year of composition | Integer | |
| Publisher | String | |
| Publisher IPI | String | Publisher Interested Parties Information code |
| Edition | String | e.g. "Revised 2010", "Score & Parts" |
| Copyright | String | |
| Rights status | Enum | Legal status for archiving/digitization/distribution — see [Rights status enum](#rights-status) |
| GEMA reportable | 3-state enum | Whether the piece is GEMA-reportable when performed ("GEMA-pflichtig" in German) — see [GEMA reportable enum](#gema-reportable) |
| Arrangement publisher | String | Publisher holding rights to this specific arrangement, if different from the original publisher |
| Arrangement rights until | Integer (year) | Year until which arrangement rights are held, e.g. for rental/hire-only material |
| Source | String | Provenance — where the piece came from (e.g. a publisher, a donation) |
| ISWC | String | International Standard Musical Work Code |
| GEMA work number | String | German GEMA identifier |
| Additional notes | Free text | General remarks, performance notes |
| Tags | Set\<String\> | Free-form labels (e.g. "christmas", "outdoor", "opening") |
| Rating | 1–5 | User rating |
| Favorite | Boolean | Quick-access flag |

## Actions

- **Create / edit / delete** via a dedicated full-page form. At create time the sheet can optionally be added to an existing [collection](collections.md) directly (`collectionId` on the create request). Deleting a sheet also removes its collection memberships.
- **Tags** can be added and removed individually without editing the full sheet.
- **Favorite** can be toggled directly from the list and detail views.
- **Fingerprint-based deduplication** — creating a sheet with identical core metadata is rejected at the database level (see [Storage & Deduplication](../architecture/concepts/storage-and-deduplication.md)).

## Enums

### Genre
Describes the structural/formal category of the piece.

`MARCH` · `MARCHING_SHOW` · `CONCERT_WORK` · `OVERTURE` · `SUITE` · `SYMPHONY` ·
`FANTASY` · `VARIATIONS` · `DANCE` · `WALTZ` · `POLKA` · `FOLK_SONG` ·
`HYMN_CHORALE` · `FILM_MUSIC` · `SHOW_MUSIC` · `POP_ROCK` · `JAZZ` ·
`LATIN` · `CHRISTMAS` · `SACRED` · `SOLO_WITH_BAND`

### Style
Describes the aesthetic or period character (optional, complementary to genre).

`CLASSICAL` · `ROMANTIC` · `MODERN` · `CONTEMPORARY` · `POP` · `ROCK` ·
`FUNK` · `SWING` · `LATIN` · `TRADITIONAL` · `FOLKLORISTIC` · `EXPERIMENTAL`

### Difficulty level

| Grade | Label |
|-------|-------|
| 1 | Very Easy |
| 2 | Easy |
| 3 | Medium |
| 4 | Advanced |
| 5 | Difficult |
| 6 | Very Difficult |

### Rights status

Legal status of a sheet with respect to archiving, digitization, and distribution —
distinct from the free-text `copyright` field or the GEMA work number. Shown as a
colored badge on the sheet detail page.

| Value | Meaning |
|-------|---------|
| `UNKNOWN` | No rights research has been done yet, or the status is genuinely unclear (default) |
| `PUBLIC_DOMAIN` | No copyright restrictions apply |
| `LICENSED` | Covered by a license (e.g. purchased performance/print license) that permits normal use |
| `PERMITTED_ARCHIVE` | Not licensed, but the publisher/arranger gave explicit permission to archive a copy — short of a full license |
| `RESTRICTED` | Explicit permission must be sought before each new use (performance, copying, distribution) |
| `NO_DIGITALIZATION` | The physical original may be archived and catalogued, but digitizing/scanning it is specifically prohibited (common for rental/hire-only orchestral material) |

### GEMA reportable

Whether a sheet is subject to GEMA reporting ("GEMA-pflichtig" in German) when performed.

`UNKNOWN` (default) · `YES` · `NO`

### Tonality

Musical key of the piece. Fixed enum (not free text) covering the full circle of fifths —
15 major keys, 15 minor keys (with enharmonic spellings), plus `ATONAL` for pieces with no
tonal center.

`C_MAJOR` · `G_MAJOR` · `D_MAJOR` · `A_MAJOR` · `E_MAJOR` · `B_MAJOR` · `F_SHARP_MAJOR` ·
`C_SHARP_MAJOR` · `F_MAJOR` · `B_FLAT_MAJOR` · `E_FLAT_MAJOR` · `A_FLAT_MAJOR` ·
`D_FLAT_MAJOR` · `G_FLAT_MAJOR` · `C_FLAT_MAJOR` · `A_MINOR` · `E_MINOR` · `B_MINOR` ·
`F_SHARP_MINOR` · `C_SHARP_MINOR` · `G_SHARP_MINOR` · `D_SHARP_MINOR` · `A_SHARP_MINOR` ·
`D_MINOR` · `G_MINOR` · `C_MINOR` · `F_MINOR` · `B_FLAT_MINOR` · `E_FLAT_MINOR` ·
`A_FLAT_MINOR` · `ATONAL`

## Related

- [Instrumentations](instrumentations.md) — the parts belonging to a sheet
- [AI Data Enrichment](ai-enrichment.md) — AI suggestions for missing metadata
