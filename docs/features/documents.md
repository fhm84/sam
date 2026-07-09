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

- **Content-addressed storage** — files are identified by SHA-256 checksum. Uploading the same file twice increments a reference count instead of duplicating storage (see [Storage & Deduplication](../architecture/concepts/storage-and-deduplication.md)).
- **Pluggable storage backend** — local filesystem or AWS S3.
- **ETag-based HTTP caching** on download.
- **Linking & relinking** — a document in the unlinked pool can be assigned to a sheet or instrumentation at any time. Existing links can be edited (target sheet, target instrumentation, attachment type).
- **Batch download** — select multiple documents for download as a ZIP archive or as a **merged PDF** (when all selected files are PDFs).
- **Unlinked pool** (`/uploads`) — staging area showing all documents not yet assigned, with classify and assign actions.

## Related

- [AI Classification](ai-classification.md) — turning an uploaded document into archive entities
- [Event Log](event-log.md) — downloads are tracked as read events
