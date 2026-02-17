package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * RESTful API for managing ensemble definitions. An ensemble describes a target instrumentation
 * (e.g. "Brass Quintet") against which sheet music completeness can be evaluated.
 */
@Path("ensembles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface EnsemblesResource {

    /**
     * Finds ensembles based on the provided filter criteria.
     *
     * @param filterRequest pagination and filter parameters
     * @return a paginated response containing matching ensembles
     */
    @GET
    PaginatedResponse<Ensemble> findEnsembles(final @BeanParam EnsembleFilterRequest filterRequest);

    /**
     * Creates a new ensemble.
     *
     * @param ensemble the ensemble data
     * @return the created ensemble with generated ID
     */
    @POST
    Ensemble add(final CreateEnsemble ensemble);

    /**
     * Loads a specific ensemble by its ID, including all voices and their options.
     *
     * @param ensembleId the ID of the ensemble
     * @return the ensemble with the specified ID
     */
    @GET
    @Path("{ensembleId}")
    Ensemble load(final @PathParam("ensembleId") String ensembleId);

    /**
     * Updates an existing ensemble's name and description.
     *
     * @param ensembleId the ID of the ensemble to update
     * @param ensemble the updated ensemble data
     */
    @PUT
    @Path("{ensembleId}")
    void update(final @PathParam("ensembleId") String ensembleId, final Ensemble ensemble);

    /**
     * Deletes an ensemble and all its voices and options.
     *
     * @param ensembleId the ID of the ensemble to delete
     */
    @DELETE
    @Path("{ensembleId}")
    void delete(final @PathParam("ensembleId") String ensembleId);

    /**
     * Provides access to the voices sub-resource for a specific ensemble.
     *
     * @param ensembleId the ID of the ensemble
     * @return the voices resource
     */
    @Path("{ensembleId}/voices")
    EnsembleVoicesResource voices(final @PathParam("ensembleId") String ensembleId);
}
