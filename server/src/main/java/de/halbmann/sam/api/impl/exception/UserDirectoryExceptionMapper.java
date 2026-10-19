package de.halbmann.sam.api.impl.exception;

import de.halbmann.sam.core.entity.ErrorResponse;
import de.halbmann.sam.security.UserDirectoryException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Maps a failing OIDC provider user API (unreachable, rejected API key) to HTTP 502: SAM itself is
 * fine, the upstream provider is not.
 */
@Slf4j
@Provider
public class UserDirectoryExceptionMapper implements ExceptionMapper<UserDirectoryException> {

    @Override
    public Response toResponse(UserDirectoryException exception) {
        log.atWarn().setCause(exception).log("User directory call failed");
        ErrorResponse error =
                new ErrorResponse(Response.Status.BAD_GATEWAY.getStatusCode(), "Bad Gateway", exception.getMessage());
        return Response.status(Response.Status.BAD_GATEWAY).entity(error).build();
    }
}
