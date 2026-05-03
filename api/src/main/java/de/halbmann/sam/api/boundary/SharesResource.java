package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shares.CreateShareRequest;
import de.halbmann.sam.api.entity.shares.ShareResponse;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;

/**
 * REST interface for managing share links.
 *
 * <p>All endpoints require an authenticated OIDC token. The share list and revoke operations
 * are automatically scoped to the calling user — a user can only see and revoke their own shares.
 *
 * <p>The public (unauthenticated) endpoints for accessing a share by token are defined in
 * {@code PublicShareResource} in the server module.
 */
@Path("shares")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SharesResource {

    /**
     * Creates a new share link for the given resource.
     *
     * @param request resource type, resource ID, and optional expiry
     * @return the created share, including the token UUID used to build the public URL
     */
    @POST
    ShareResponse create(@Valid CreateShareRequest request);

    /**
     * Returns all shares created by the calling user, ordered by creation date descending.
     * The response includes a human-readable {@code resourceLabel} resolved at query time.
     */
    @GET
    PaginatedResponse<ShareResponse> list();

    /**
     * Revokes the share with the given ID.
     * Returns 404 if the share does not exist or was not created by the calling user.
     *
     * @param id the share token UUID
     */
    @DELETE
    @Path("{id}")
    void revoke(@PathParam("id") UUID id);
}
