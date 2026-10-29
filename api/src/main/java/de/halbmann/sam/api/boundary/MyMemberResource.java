package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.members.OwnMemberData;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/** The calling user's own member data (GDPR right of access). */
@Path("me/member")
@Produces(MediaType.APPLICATION_JSON)
public interface MyMemberResource {

    /**
     * Returns the member profile linked to the caller's login, with personal data and seats.
     *
     * @return the caller's own member data; 404 when no member is linked to the login
     */
    @GET
    OwnMemberData get();
}
