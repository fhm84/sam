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
}
