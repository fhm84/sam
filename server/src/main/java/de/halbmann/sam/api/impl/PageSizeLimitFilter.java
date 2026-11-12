package de.halbmann.sam.api.impl;

import de.halbmann.sam.core.entity.ErrorResponse;
import de.halbmann.sam.security.CurrentUserService;
import de.halbmann.sam.security.Roles;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Limits the {@code size} query parameter of the paginated endpoints. A page of at most
 * {@value #MAX_PAGE_SIZE} entries is open to everyone; larger pages and {@code size=-1} ("no
 * paging", see {@link de.halbmann.sam.api.entity.shared.PaginationRequest}) are left to music
 * librarians and admins, whose exports and imports (the {@code cli}) read whole lists in one call.
 * Anyone else gets HTTP 400, so that a single request can't make the server load and serialize an
 * entire table.
 *
 * <p>Applies to every endpoint by the parameter's name, whichever class binds it.
 */
@Provider
public class PageSizeLimitFilter implements ContainerRequestFilter {

    /** Largest page any user may request; the UI asks for 200 at most. */
    public static final int MAX_PAGE_SIZE = 500;

    @Inject
    CurrentUserService currentUser;

    @Override
    public void filter(ContainerRequestContext request) {
        String raw = request.getUriInfo().getQueryParameters().getFirst("size");
        if (raw == null) {
            return;
        }
        int size;
        try {
            size = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return; // not a number: the parameter binding answers that
        }
        if (size >= 0 && size <= MAX_PAGE_SIZE) {
            return;
        }
        if (currentUser.hasRole(Roles.MUSIC_LIBRARIAN) || currentUser.hasRole(Roles.ADMIN)) {
            return;
        }
        ErrorResponse error = new ErrorResponse(
                Response.Status.BAD_REQUEST.getStatusCode(),
                "Bad Request",
                "size must be between 0 and " + MAX_PAGE_SIZE);
        request.abortWith(Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build());
    }
}
