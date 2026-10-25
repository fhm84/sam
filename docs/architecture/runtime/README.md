# Runtime Views

Sequence diagrams for the four core workflows. The narrative version of each flow lives
in [Stakeholders §5](../../stakeholders.md#5-key-flows). The static structure is in
[Building Blocks](../building-blocks.md#inside-the-server).

| Flow | Summary |
|------|---------|
| [1 — AI-assisted archival](#flow-1-ai-assisted-archival) | Scan → upload → classify → review → apply → record location/condition |
| [2 — Concert programme check](#flow-2-concert-programme-check) | Coverage badges → per-voice breakdown → build setlist |
| [3 — Musician retrieves part](#flow-3-musician-retrieves-part) | Search → instrumentations tab → archive location → download |
| [4 — Share creation & guest access](#flow-4-share-creation-and-guest-access) | Create token → distribute URL → unauthenticated access → event log |

## Flow 1: AI-assisted archival

A new physical score, archived by the music librarian.

```mermaid
sequenceDiagram
    actor Lib as Music librarian
    participant UI as SAM UI
    participant Server as SAM Server
    participant LLM as LLM provider

    Lib->>UI: scans score to PDF,<br/>uploads to staging area
    UI->>Server: POST /api/documents
    Server-->>UI: document stored (unlinked)

    Lib->>UI: clicks "Classify"
    UI->>Server: POST /api/documents/{id}/classify
    Server->>Server: extract text (PDFBox)
    alt text extraction insufficient (scanned/image-only)
        Server->>LLM: vision fallback
        LLM-->>Server: detected metadata
    end
    Server->>Server: match musicians/instruments<br/>(pg_trgm trigram similarity)
    Server-->>UI: SheetClassification
    UI-->>Lib: review form pre-filled

    Lib->>UI: adjusts fields, confirms
    UI->>Server: POST /api/documents/{id}/apply
    Server->>Server: create/resolve Sheet,<br/>create Instrumentation,<br/>link Attachment
    Server-->>UI: ClassificationApplyResult
    UI-->>Lib: sheet detail opens

    Lib->>UI: opens instrumentation,<br/>enters physical location,<br/>sets condition = GOOD
    UI->>Server: PUT /api/sheets/{sheetId}/instrumentations/{id}
    Server-->>UI: saved
```

## Flow 2: Concert programme check

The conductor checks what the ensemble can play and builds a setlist.

```mermaid
sequenceDiagram
    actor Cond as Conductor
    participant UI as SAM UI
    participant Server as SAM Server

    Cond->>UI: opens Sheets list
    UI->>Server: GET /api/sheets?ensemble={id}
    Server-->>UI: sheets + snapshot badges
    UI-->>Cond: list with COMPLETE / PLAYABLE / INCOMPLETE badges

    Cond->>UI: clicks incomplete piece
    UI->>Server: GET /api/sheets/{id}/coverage?ensemble={id}
    Server->>Server: live evaluation
    Server-->>UI: coverage detail<br/>(missing voices, score breakdown)
    UI-->>Cond: shows coverage detail

    Cond->>UI: creates setlist
    UI->>Server: POST /api/sheet-collections
    Cond->>UI: adds pieces
    UI->>Server: POST /api/sheet-collections/{id}/items
    Server-->>UI: setlist ready
```

## Flow 3: Musician retrieves part

```mermaid
sequenceDiagram
    actor Mus as Musician
    participant UI as SAM UI
    participant Server as SAM Server

    Mus->>UI: searches by title
    UI->>Server: GET /api/sheets?q=...
    Server-->>UI: results
    UI-->>Mus: results

    Mus->>UI: opens sheet detail →<br/>Instrumentations tab
    UI-->>Mus: table: instrument, part label,<br/>archive location, condition

    Mus->>UI: expands row for their instrument
    UI-->>Mus: attached files listed

    Mus->>UI: downloads part PDF
    UI->>Server: GET /api/documents/{id}
    Server-->>UI: file
    UI-->>Mus: file download
```

## Flow 4: Share creation and guest access

```mermaid
sequenceDiagram
    actor Lib as Music librarian
    participant UI as SAM UI
    participant Server as SAM Server
    actor Guest

    Lib->>UI: Shares → New share<br/>(resource + expiry)
    UI->>Server: POST /api/shares
    Server->>Server: create token,<br/>creator = caller
    Server-->>UI: ShareResponse
    UI-->>Lib: public URL shown

    Lib->>Guest: sends the link,<br/>e.g. via a messenger

    Guest->>UI: opens /share/{token} (no login)
    UI->>Server: GET /api/public/shares/{token}
    Server->>Server: validate: not expired/revoked
    Server->>Server: event_log SHARE_ACCESSED<br/>(shareTokenId set,<br/>userId/username null)
    Server-->>UI: shared resource
    UI-->>Guest: renders resource
```
