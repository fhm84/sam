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

## Error Responses

Errors are returned as an `ErrorResponse` (`status`, `error`, `message`) by the exception
mappers in `server/.../api/impl/exception/`:

| Exception | Status | When |
|-----------|--------|------|
| `ValidationException` | 400 | Invalid request parameters or content (e.g. an image that cannot be decoded for classification) |
| `IllegalArgumentException` from `UUID.fromString` | 400 | Malformed ID in the path; any other `IllegalArgumentException` is a bug and stays a logged 500 |
| `JsonbException` while reading the request body | 400 | The body doesn't fit the parameter type (e.g. an object where a list is expected); one thrown anywhere else stays a logged 500 |
| `EntityNotFoundException` | 404 | Referenced entity does not exist |
| `ConflictException` | 409 | The request conflicts with existing data (e.g. deleting a document that is still linked, or a person who is still credited) |
| `StorageException` | 500 | The file store failed |

Request DTOs are checked with Bean Validation: an `api` interface parameter marked `@Valid`
(e.g. the create endpoints for sheets, ensembles and instruments) answers 400 with a
`violations` list naming each field, before the resource method runs. Credits on a sheet
(`composer`, `arranger`) are references (an existing person's ID or a name) and are not
cascaded, since an ID-only reference has no name.

Services throw these exceptions rather than building JAX-RS `Response`s. Some resource methods
still return a bare status without a body (e.g. 400/404 in `DocumentsResourceImpl` and
`PublicShareResourceImpl`), and a few throw JAX-RS `NotFoundException` directly.

## Related

- [Building Blocks](../building-blocks.md) — module dependency graph
- [API surface summary](../../features/api-surface.md) — endpoint overview
