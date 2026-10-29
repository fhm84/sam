package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.members.Member;
import de.halbmann.sam.api.entity.members.MemberFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * REST API for the band's members: the profiles of persons who belong to the band, with their
 * contact details, instruments and login link.
 */
@Path("members")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface MembersResource {

    /**
     * Returns a paginated list of members, sorted by name.
     *
     * @param filterRequest name and status filters and pagination parameters
     * @return paginated list of matching members
     */
    @GET
    PaginatedResponse<Member> findMembers(@BeanParam MemberFilterRequest filterRequest);

    /**
     * Creates a member profile, for an existing person (when {@code id} is set) or a new one.
     *
     * @param member the member to create
     * @return the persisted member
     */
    @POST
    Member add(Member member);

    /**
     * Loads a single member by their ID.
     *
     * @param memberId the member ID (the person's ID)
     * @return the member
     */
    @GET
    @Path("{memberId}")
    Member load(@PathParam("memberId") String memberId);

    /**
     * Updates an existing member.
     *
     * @param memberId the member ID
     * @param member   the updated member data
     */
    @PUT
    @Path("{memberId}")
    void update(@PathParam("memberId") String memberId, Member member);

    /**
     * Erases a member: their profile, instruments, ensemble seats and the audit history of these.
     * The person stays if it is credited on a sheet.
     *
     * @param memberId the member ID
     */
    @DELETE
    @Path("{memberId}")
    void delete(@PathParam("memberId") String memberId);

    /** Links the member to a login via OIDC subject claim. */
    @PUT
    @Path("{memberId}/user/{userId}")
    void linkUser(@PathParam("memberId") String memberId, @PathParam("userId") String userId);

    /** Removes the login link from this member. */
    @DELETE
    @Path("{memberId}/user")
    void unlinkUser(@PathParam("memberId") String memberId);
}
