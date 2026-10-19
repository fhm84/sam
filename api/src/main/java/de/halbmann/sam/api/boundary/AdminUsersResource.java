package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.admin.UserDirectoryInfo;
import de.halbmann.sam.api.entity.admin.UserInfo;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Admin-only endpoint for looking up the OIDC provider's users (Pocket ID or Keycloak). Used to
 * link musicians to user accounts.
 */
@Path("admin/users")
@Produces(MediaType.APPLICATION_JSON)
public interface AdminUsersResource {

    /**
     * Searches users by name, username, or email fragment. Answers 501 when the provider's user
     * search is not configured (see {@link #getDirectory()}).
     *
     * @param search the search string
     * @return list of matching user records
     */
    @GET
    List<UserInfo> searchUsers(@QueryParam("search") String search);

    /**
     * Tells whether user search is available for the active provider.
     *
     * @return the active provider and whether it can be searched
     */
    @GET
    @Path("directory")
    UserDirectoryInfo getDirectory();

    /**
     * Loads a single user by their subject ID. Without user search, only the ID is known and is
     * returned as the username.
     *
     * @param userId the user ID (subject claim)
     * @return the user record
     */
    @GET
    @Path("{userId}")
    UserInfo getUser(@PathParam("userId") String userId);
}
