# Runtime Views

Sequence diagrams for the four core workflows, one `.puml` file per flow
(viewable in any PlantUML renderer or IDE plugin). The narrative version of
each flow lives in [Stakeholders §5](../../stakeholders.md#5-key-flows).

| Flow | Diagram | Summary |
|------|---------|---------|
| 1 — AI-assisted archival | [flow-1-ai-assisted-archival.puml](flow-1-ai-assisted-archival.puml) | Scan → upload → classify → review → apply → record location/condition |
| 2 — Concert programme check | [flow-2-concert-programme-check.puml](flow-2-concert-programme-check.puml) | Coverage badges → per-voice breakdown → build setlist |
| 3 — Musician retrieves part | [flow-3-musician-retrieves-part.puml](flow-3-musician-retrieves-part.puml) | Search → instrumentations tab → archive location → download |
| 4 — Share creation & guest access | [flow-4-share-creation-guest-access.puml](flow-4-share-creation-guest-access.puml) | Create token → distribute URL → unauthenticated access → event log |

The static component diagram is [../architecture.puml](../architecture.puml).
