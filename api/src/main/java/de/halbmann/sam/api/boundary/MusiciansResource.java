package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.Musician;
import de.halbmann.sam.api.entity.MusicianFilterRequest;
import de.halbmann.sam.api.entity.PaginatedResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("musicians")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "musicians-api")
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
