package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.Instrumentation;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "instrumentations-api")
public interface InstrumentationsResource {

    @GET
    List<Instrumentation> listAll();

    @GET
    @Path("{instrumentationId}")
    Instrumentation get(final @PathParam("instrumentationId") String instrumentationId);

    @POST
    void add(final Instrumentation instrumentation);

    @PUT
    @Path("{instrumentationId}")
    void update(final @PathParam("instrumentationId") String instrumentationId, final Instrumentation instrumentation);

    @DELETE
    @Path("{instrumentationId}")
    void delete(final @PathParam("instrumentationId") String instrumentationId);

    @Path("{instrumentationId}/documents")
    DocumentsResource documents(final @PathParam("instrumentationId") String instrumentationId);

}
