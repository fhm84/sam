package de.halbmann.sam.api.impl.exception;

import de.halbmann.sam.core.entity.ErrorResponse;
import de.halbmann.sam.core.exception.StorageException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * JAX-RS provider mapping {@link StorageException} to HTTP 500 (including logging).
 */
@Slf4j
@Provider
public class StorageExceptionMapper implements ExceptionMapper<StorageException> {

    @Override
    public Response toResponse(StorageException exception) {
        log.error("Storage error: {}", exception.getMessage(), exception);
        ErrorResponse error = new ErrorResponse(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Internal Server Error", exception.getMessage());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .build();
    }
}
