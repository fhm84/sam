package de.halbmann.sam.api.impl.exception;

import de.halbmann.sam.core.entity.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Maps a malformed ID in the request (path parameters are parsed with {@code UUID.fromString}) to
 * HTTP 400 instead of 500. Only the messages {@code UUID.fromString} produces are treated as client
 * errors; any other {@link IllegalArgumentException} is a bug and stays a logged 500.
 */
@Slf4j
@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        if (isMalformedUuid(exception)) {
            ErrorResponse error = new ErrorResponse(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    "Bad Request",
                    "Malformed ID: " + exception.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }
        log.atError().setCause(exception).log("Unhandled IllegalArgumentException");
        ErrorResponse error =
                new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Internal Server Error", null);
        return Response.serverError().entity(error).build();
    }

    private static boolean isMalformedUuid(IllegalArgumentException exception) {
        String message = exception.getMessage();
        return message != null
                && (message.startsWith("Invalid UUID string") || "UUID string too large".equals(message));
    }
}
