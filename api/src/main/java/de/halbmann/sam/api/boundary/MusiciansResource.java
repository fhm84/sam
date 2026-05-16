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

    @GET
    PaginatedResponse<Musician> findMusicians(@BeanParam MusicianFilterRequest filterRequest);

    @POST
    Musician add(Musician musician);

    @GET
    @Path("{musicianId}")
    Musician load(@PathParam("musicianId") String musicianId);

    @PUT
    @Path("{musicianId}")
    void update(@PathParam("musicianId") String musicianId, Musician musician);

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
