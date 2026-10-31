# Documents & Attachments

SAM manages physical files (PDFs, audio, images, etc.) separately from metadata.

## Upload

Files are uploaded to a **staging area** (unlinked pool) or directly to a sheet or instrumentation. Supported via drag-and-drop or file picker.

## Attachment types

| Type | Description |
|------|-------------|
| `FULL_SCORE` | Complete conductor's score |
| `PART` | Individual instrument part |
| `COVER` | Title page / decorative cover |
| `LYRICS` | Text-only lyrics document |
| `MIDI` | MIDI playback file |
| `AUDIO` | MP3 / WAV recording |
| `ANNOTATIONS` | Score with markings, fingerings |
| `IMAGE` | Scanned JPG / PNG |
| `ANALYSIS` | Harmonic or structural analysis |
| `TRANSCRIPTION` | Manually transcribed version |
| `EXTERNAL_LINK` | URL to an external resource |
| `MUSIC_XML` | MusicXML / MXL exchange format |
| `OTHER` / `UNSPECIFIED` | Catch-all |

## Document features

- **Deduplicated storage** — each file is stored once under a random path; its SHA-256 checksum identifies it, so uploading the same file twice reuses the stored document instead of duplicating it (see [Storage & Deduplication](../architecture/concepts/storage-and-deduplication.md)).
- **Integrity check** — admins can re-hash every stored file on the Configuration page and see damaged, missing and orphaned files (`POST /api/admin/storage/integrity-check`).
- **Pluggable storage backend** — local filesystem or AWS S3.
- **ETag-based HTTP caching** on download.
- **Linking & relinking** — a document in the unlinked pool can be assigned to a sheet or instrumentation at any time. Existing links can be edited (target sheet, target instrumentation, attachment type).
- **Batch download** — select multiple documents for download as a ZIP archive or as a **merged PDF** (when all selected files are PDFs).
- **Download names** — a downloaded file is named after the sheet, not the scanner: a part as `<title> – <instrument>` (`1000 Träume weit – Tenorhorn in B 1.pdf`, with `(<type>)` added for non-parts such as audio), a document of the sheet as `<title> – <type>` (`… – Partitur.pdf`). The same names apply to single downloads, share links and the files inside ZIPs; the type follows the app's language (sent as `Accept-Language`). Umlauts stay, characters file systems reject are replaced. A document not attached anywhere keeps its original file name. Built in `DownloadNames`.
- **Unlinked pool** (`/uploads`) — staging area showing all documents not yet assigned, with classify and assign actions.

## Related

- [AI Classification](ai-classification.md) — turning an uploaded document into archive entities
- [Event Log](event-log.md) — downloads are tracked as read events
