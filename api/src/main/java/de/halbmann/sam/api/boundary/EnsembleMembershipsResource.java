package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.ensembles.CreateEnsembleMembership;
import de.halbmann.sam.api.entity.ensembles.EnsembleMembership;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * RESTful API for managing ensemble memberships. A membership links a musician to an ensemble,
 * optionally specifying the voice and instrument they play.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface EnsembleMembershipsResource {

    /**
     * Lists all members of the ensemble.
     *
     * @return all memberships for this ensemble, sorted by musician name
     */
    @GET
    List<EnsembleMembership> list();

    /**
     * Adds a musician to the ensemble.
     *
     * @param membership the membership to create
     * @return the created membership with generated ID
     */
    @POST
    EnsembleMembership add(CreateEnsembleMembership membership);

    /**
     * Updates an existing membership (voice, instrument, conductor flag).
     *
     * @param memberId the ID of the membership to update
     * @param membership the updated membership data
     * @return the updated membership
     */
    @PUT
    @Path("{memberId}")
    EnsembleMembership update(@PathParam("memberId") String memberId, EnsembleMembership membership);

    /**
     * Removes a musician from the ensemble.
     *
     * @param memberId the ID of the membership to delete
     */
    @DELETE
    @Path("{memberId}")
    void delete(@PathParam("memberId") String memberId);
}
