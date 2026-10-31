package de.halbmann.sam.api.impl.exception;

import de.halbmann.sam.core.entity.ErrorResponse;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

/**
 * Maps a request body that JSON-B can't read into the parameter type (an object where a list is
 * expected, a string for a number, broken JSON) to HTTP 400 instead of 500. Only exceptions thrown
 * while a {@code MessageBodyReader} reads the request are client errors; a {@link JsonbException}
 * from anywhere else (serialising a response, application code) is a bug and stays a logged 500.
 */
@Slf4j
@Provider
public class JsonbExceptionMapper implements ExceptionMapper<JsonbException> {

    @Override
    public Response toResponse(JsonbException exception) {
        if (isReadingRequestBody(exception)) {
            ErrorResponse error = new ErrorResponse(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    "Bad Request",
                    "Malformed request body: " + exception.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }
        log.atError().setCause(exception).log("Unhandled JsonbException");
        ErrorResponse error =
                new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Internal Server Error", null);
        return Response.serverError().entity(error).build();
    }

    static boolean isReadingRequestBody(Throwable exception) {
        return Arrays.stream(exception.getStackTrace())
                .anyMatch(frame -> "readFrom".equals(frame.getMethodName())
                        && frame.getClassName().endsWith("MessageBodyReader"));
    }
}
