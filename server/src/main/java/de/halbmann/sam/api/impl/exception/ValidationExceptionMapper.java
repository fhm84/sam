package de.halbmann.sam.api.impl.exception;

import de.halbmann.sam.core.entity.ErrorResponse;
import de.halbmann.sam.core.exception.ValidationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS provider mapping {@link ValidationException} to HTTP 400.
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException exception) {
        ErrorResponse error =
                new ErrorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "Bad Request", exception.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }
}
