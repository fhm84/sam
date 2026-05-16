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

    @GET
    List<UserInfo> searchUsers(@QueryParam("search") String search);

    @GET
    @Path("{userId}")
    UserInfo getUser(@PathParam("userId") String userId);
}
