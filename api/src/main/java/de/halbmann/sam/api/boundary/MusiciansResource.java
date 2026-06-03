package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.musicians.MusicianFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * REST API for managing musicians. A musician record represents a person referenced as composer,
 * arranger, or ensemble member — it is a shared canonical entry, not a per-sheet copy.
 */
@Path("musicians")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface MusiciansResource {

    /**
     * Returns a paginated list of musicians matching the given filter criteria.
     *
     * @param filterRequest name filter and pagination parameters
     * @return paginated list of matching musicians
     */
    @GET
    PaginatedResponse<Musician> findMusicians(@BeanParam MusicianFilterRequest filterRequest);

    /**
     * Creates a new musician record.
     *
     * @param musician the musician to create
     * @return the persisted musician including its generated ID
     */
    @POST
    Musician add(Musician musician);

    /**
     * Loads a single musician by their ID.
     *
     * @param musicianId the musician ID
     * @return the musician
     */
    @GET
    @Path("{musicianId}")
    Musician load(@PathParam("musicianId") String musicianId);

    /**
     * Updates an existing musician record.
     *
     * @param musicianId the musician ID
     * @param musician   the updated musician data
     */
    @PUT
    @Path("{musicianId}")
    void update(@PathParam("musicianId") String musicianId, Musician musician);

    /**
     * Deletes a musician by their ID.
     *
     * @param musicianId the musician ID
     */
    @DELETE
    @Path("{musicianId}")
    void delete(@PathParam("musicianId") String musicianId);

    /** Links the musician to an authenticated user account via OIDC subject claim. */
    @PUT
    @Path("{musicianId}/user/{userId}")
    void linkUser(@PathParam("musicianId") String musicianId, @PathParam("userId") String userId);

    /** Removes the user-account link from this musician. */
    @DELETE
    @Path("{musicianId}/user")
    void unlinkUser(@PathParam("musicianId") String musicianId);
}
