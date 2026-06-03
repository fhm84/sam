package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.admin.UserInfo;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Admin-only endpoint for looking up Keycloak users. Used to link musicians to user accounts.
 */
@Path("admin/users")
@Produces(MediaType.APPLICATION_JSON)
public interface AdminUsersResource {

    /**
     * Searches Keycloak users by name, username, or email fragment.
     *
     * @param search the search string
     * @return list of matching user records
     */
    @GET
    List<UserInfo> searchUsers(@QueryParam("search") String search);

    /**
     * Loads a single Keycloak user by their subject ID.
     *
     * @param userId the Keycloak user ID (subject claim)
     * @return the user record
     */
    @GET
    @Path("{userId}")
    UserInfo getUser(@PathParam("userId") String userId);
}
