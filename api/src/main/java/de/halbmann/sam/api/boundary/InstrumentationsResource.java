package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.sheets.CreateInstrumentation;
import de.halbmann.sam.api.entity.sheets.Instrumentation;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Sub-resource for managing the instrument parts (instrumentations) of a sheet music entry. Each
 * instrumentation links a sheet to a specific instrument and voice, and can carry its own
 * attachments (e.g. individual part PDFs).
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface InstrumentationsResource {

    @GET
    List<Instrumentation> listAll();

    @GET
    @Path("{instrumentationId}")
    Instrumentation get(@PathParam("instrumentationId") String instrumentationId);

    @POST
    void add(CreateInstrumentation instrumentation);

    @POST
    @Path("bulk")
    void add(List<CreateInstrumentation> instrumentations);

    @PUT
    @Path("{instrumentationId}")
    void update(@PathParam("instrumentationId") String instrumentationId, Instrumentation instrumentation);

    @DELETE
    @Path("{instrumentationId}")
    void delete(@PathParam("instrumentationId") String instrumentationId);

    @Path("{instrumentationId}/documents")
    DocumentsResource documents(@PathParam("instrumentationId") String instrumentationId);
}
