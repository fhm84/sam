package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.ensembles.CreateEnsemble;
import de.halbmann.sam.api.entity.ensembles.Ensemble;
import de.halbmann.sam.api.entity.ensembles.EnsembleCoverageStatus;
import de.halbmann.sam.api.entity.ensembles.EnsembleFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
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
    PaginatedResponse<Ensemble> findEnsembles(@BeanParam EnsembleFilterRequest filterRequest);

    /**
     * Creates a new ensemble.
     *
     * @param ensemble the ensemble data
     * @return the created ensemble with generated ID
     */
    @POST
    Ensemble add(CreateEnsemble ensemble);

    /**
     * Loads a specific ensemble by its ID, including all voices and their options.
     *
     * @param ensembleId the ID of the ensemble
     * @return the ensemble with the specified ID
     */
    @GET
    @Path("{ensembleId}")
    Ensemble load(@PathParam("ensembleId") String ensembleId);

    /**
     * Updates an existing ensemble's name and description.
     *
     * @param ensembleId the ID of the ensemble to update
     * @param ensemble the updated ensemble data
     */
    @PUT
    @Path("{ensembleId}")
    void update(@PathParam("ensembleId") String ensembleId, Ensemble ensemble);

    /**
     * Deletes an ensemble and all its voices and options.
     *
     * @param ensembleId the ID of the ensemble to delete
     */
    @DELETE
    @Path("{ensembleId}")
    void delete(@PathParam("ensembleId") String ensembleId);

    /**
     * Provides access to the voices sub-resource for a specific ensemble.
     *
     * @param ensembleId the ID of the ensemble
     * @return the voices resource
     */
    @Path("{ensembleId}/voices")
    EnsembleVoicesResource voices(@PathParam("ensembleId") String ensembleId);

    /**
     * Provides access to the members sub-resource for a specific ensemble.
     *
     * @param ensembleId the ID of the ensemble
     * @return the members resource
     */
    @Path("{ensembleId}/members")
    EnsembleMembershipsResource members(@PathParam("ensembleId") String ensembleId);

    /**
     * Precomputes coverage for all sheets against the given ensemble and stores the results as a
     * snapshot. Subsequent calls overwrite the existing snapshot.
     *
     * @param ensembleId the ID of the ensemble
     * @return the computed snapshot status including sheet count and timestamp
     */
    @POST
    @Path("{ensembleId}/coverage/compute")
    EnsembleCoverageStatus computeCoverage(@PathParam("ensembleId") String ensembleId);

    /**
     * Returns the current coverage snapshot status for the given ensemble, or 404 if no snapshot
     * has been computed yet.
     *
     * @param ensembleId the ID of the ensemble
     * @return the snapshot status
     */
    @GET
    @Path("{ensembleId}/coverage/status")
    EnsembleCoverageStatus getCoverageStatus(@PathParam("ensembleId") String ensembleId);
}
