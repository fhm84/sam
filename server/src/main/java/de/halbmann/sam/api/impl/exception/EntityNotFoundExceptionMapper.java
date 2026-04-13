package de.halbmann.sam.api.impl.exception;

import de.halbmann.sam.core.entity.ErrorResponse;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS provider mapping {@link EntityNotFoundException} to HTTP 404.
 */
@Provider
public class EntityNotFoundExceptionMapper implements ExceptionMapper<EntityNotFoundException> {

    @Override
    public Response toResponse(EntityNotFoundException exception) {
        ErrorResponse error =
                new ErrorResponse(Response.Status.NOT_FOUND.getStatusCode(), "Not Found", exception.getMessage());
        return Response.status(Response.Status.NOT_FOUND).entity(error).build();
    }
}
