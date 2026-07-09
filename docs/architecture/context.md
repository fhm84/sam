# Context & Scope

SAM is a web application for archiving sheet music, managing instrumentations, musicians, and collections for bands and ensembles. It helps answer questions like: *"Do we have all the parts we need to play this piece with our brass ensemble?"*

For the human-centred view (personas, use cases, flows) see [Stakeholders](../stakeholders.md).

## System Context

```
                          +----------------+
                          |   Musician /   |
                          |  Band Leader   |
                          +-------+--------+
                                  |
                     browses, uploads, manages
                                  |
                          +-------v--------+
                          |    Angular UI  |
                          | (Quarkus Quinoa)|
                          +-------+--------+
                                  |
                             REST/JSON
                                  |
    +-------------+       +-------v--------+       +----------------+
    |   CLI       +------>|   SAM Server   +------>|  PostgreSQL    |
    | (PicoCLI)   | REST  |  (Quarkus)     |  JDBC |  + pg_trgm     |
    +-------------+       +-------+--------+       |  + fuzzystrmatch|
                                  |                +----------------+
                                  |
                          +-------v--------+
                          | Storage Backend|
                          | (Local / S3)   |
                          +----------------+
                                  |
                          +-------v--------+
                          |   LLM Provider |
                          | (Ollama/OpenAI)|
                          +----------------+
```

**Users** interact via the Angular frontend or the CLI for batch operations. The server is the single backend, persisting to PostgreSQL and storing documents via a pluggable storage layer. AI classification uses LangChain4j with configurable LLM providers.

## Key Quality Goals

| Priority | Goal | Approach |
|----------|------|----------|
| 1 | Findability | Full-text search with fuzzy matching, phonetic search, trigram similarity |
| 2 | Traceability | Full audit trail via Hibernate Envers on all entities |
| 3 | Extensibility | API-first design, pluggable storage, shared interfaces across clients |

A stakeholder-derived, more detailed version of these goals lives in [Stakeholders §2](../stakeholders.md#2-quality-goals).

## Related

- [Building Blocks](building-blocks.md) — module structure
- [Deployment](deployment.md) — how the system is shipped and run
