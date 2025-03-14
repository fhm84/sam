package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.Instrumentation;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface InstrumentationsResource {

    @GET
    List<Instrumentation> listAll();

    @GET
    @Path("{instrumentationId}")
    Instrumentation get(final @PathParam("instrumentationId") String instrumentationId);

    // FIXME: add endpoint to load file (pdf)

    @POST
    void add(final Instrumentation instrumentation);

    @PUT
    @Path("{instrumentationId}")
    void update(final @PathParam("instrumentationId") String instrumentationId, final Instrumentation instrumentation);

    // FIXME: add file-upload!

    @DELETE
    @Path("{instrumentationId}")
    void delete(final @PathParam("instrumentationId") String instrumentationId);

}
