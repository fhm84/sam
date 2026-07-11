# Building Blocks

SAM is a multi-module Maven project (`de.halbmann:sam`). Each module has a clear responsibility:

```
sam (parent)
 +-- api            Contracts: JAX-RS interfaces, DTOs, enums
 +-- core           Shared business logic (exceptions, utilities)
 +-- storage        Storage abstraction
 |    +-- storage-sdk     SPI (FileSystemProvider, FileSystemWrapper)
 |    +-- storage-local   Local filesystem implementation
 |    +-- storage-s3      AWS S3 implementation
 +-- server         Quarkus runtime: REST impls, JPA entities, services
 +-- ui             Angular frontend (served via Quarkus Quinoa)
 +-- cli            PicoCLI batch import tool (REST client)
 +-- migration      Legacy-data conversion for the cli import tool
```

## Module Dependencies

```
   cli ----------> api <------- server
   migration ------^  ^              |
                      |              +----> core
                      |              +----> storage-local ─┐
                      |              +----> storage-s3    ─┴─> storage-sdk
                      |
                     ui (consumes generated TypeScript types from api)
```

- **api** defines the contract. No runtime dependencies.
- **server** implements everything. Only module with JPA, Hibernate, Flyway, LangChain4j.
- **cli** consumes the same `api` interfaces as a MicroProfile REST Client.
- **migration** depends only on `api` (for the DTOs it converts legacy data into) — its output is consumed by `cli`, not the other way around.
- **ui** receives auto-generated TypeScript types from `api` DTOs via `typescript-generator-maven-plugin`.

## Related

- [API Design](concepts/api-design.md) — how the shared-interface contract works
- [Storage & Deduplication](concepts/storage-and-deduplication.md) — the storage SPI
- [Technology Stack](tech-stack.md)
