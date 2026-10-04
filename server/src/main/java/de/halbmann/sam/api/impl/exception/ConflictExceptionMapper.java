package de.halbmann.sam.api.impl.exception;

import de.halbmann.sam.core.entity.ErrorResponse;
import de.halbmann.sam.core.exception.ConflictException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS provider mapping {@link ConflictException} to HTTP 409.
 */
@Provider
public class ConflictExceptionMapper implements ExceptionMapper<ConflictException> {

    @Override
    public Response toResponse(ConflictException exception) {
        ErrorResponse error =
                new ErrorResponse(Response.Status.CONFLICT.getStatusCode(), "Conflict", exception.getMessage());
        return Response.status(Response.Status.CONFLICT).entity(error).build();
    }
}
