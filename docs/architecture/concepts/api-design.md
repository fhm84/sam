# API Design

## API-First with Shared Interfaces

REST interfaces are defined in the `api` module using JAX-RS + MicroProfile annotations. The same interface is:
- **Implemented** by the server (`*Impl` classes)
- **Consumed** by the CLI via `@RegisterRestClient`
- **Transpiled** to TypeScript types for the Angular frontend

This ensures contract consistency across all consumers.

Because the interfaces double as REST clients, `@RolesAllowed` must never be placed on them — role enforcement lives on the `*ResourceImpl` classes only (see [Security](security.md)).

## Sub-Resource Pattern

Nested resources (instrumentations within sheets, voices within ensembles, etc.) use the JAX-RS sub-resource locator pattern:

```java
// Parent resource interface
@Path("{sheetId}/instrumentations")
InstrumentationsResource instrumentations(@PathParam("sheetId") String sheetId);

// Implementation delegates via ResourceContext
@Override
public InstrumentationsResource instrumentations(String sheetId) {
    return resourceContext.getResource(InstrumentationsResourceImpl.class);
}

// Sub-resource impl receives parent ID via @PathParam field injection
@PathParam("sheetId") String sheetId;
```

## Related

- [Building Blocks](../building-blocks.md) — module dependency graph
- [API surface summary](../../features/api-surface.md) — endpoint overview
