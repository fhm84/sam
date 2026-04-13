package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.musicians.MusicianFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("musicians")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface MusiciansResource {

    @GET
    PaginatedResponse<Musician> findMusicians(final @BeanParam MusicianFilterRequest filterRequest);

    @POST
    Musician add(final Musician musician);

    @GET
    @Path("{musicianId}")
    Musician load(final @PathParam("musicianId") String musicianId);

    @PUT
    @Path("{musicianId}")
    void update(final @PathParam("musicianId") String musicianId, final Musician musician);

    @DELETE
    @Path("{musicianId}")
    void delete(final @PathParam("musicianId") String musicianId);
}
