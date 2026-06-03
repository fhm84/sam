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

    /**
     * Returns all instrumentations for the parent sheet.
     *
     * @return list of instrumentations
     */
    @GET
    List<Instrumentation> listAll();

    /**
     * Loads a single instrumentation by its ID.
     *
     * @param instrumentationId the instrumentation ID
     * @return the instrumentation
     */
    @GET
    @Path("{instrumentationId}")
    Instrumentation get(@PathParam("instrumentationId") String instrumentationId);

    /**
     * Adds a single instrumentation to the parent sheet.
     *
     * @param instrumentation the instrumentation to add
     */
    @POST
    void add(CreateInstrumentation instrumentation);

    /**
     * Adds multiple instrumentations to the parent sheet in a single request.
     *
     * @param instrumentations the instrumentations to add
     */
    @POST
    @Path("bulk")
    void add(List<CreateInstrumentation> instrumentations);

    /**
     * Updates an existing instrumentation.
     *
     * @param instrumentationId the instrumentation ID
     * @param instrumentation   the updated instrumentation data
     */
    @PUT
    @Path("{instrumentationId}")
    void update(@PathParam("instrumentationId") String instrumentationId, Instrumentation instrumentation);

    /**
     * Deletes an instrumentation by its ID.
     *
     * @param instrumentationId the instrumentation ID
     */
    @DELETE
    @Path("{instrumentationId}")
    void delete(@PathParam("instrumentationId") String instrumentationId);

    /**
     * Provides access to the documents sub-resource for a specific instrumentation (individual part files).
     *
     * @param instrumentationId the instrumentation ID
     * @return the documents sub-resource
     */
    @Path("{instrumentationId}/documents")
    DocumentsResource documents(@PathParam("instrumentationId") String instrumentationId);
}
