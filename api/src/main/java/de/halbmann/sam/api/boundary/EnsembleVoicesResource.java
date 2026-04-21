package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.ensembles.CreateEnsembleVoice;
import de.halbmann.sam.api.entity.ensembles.EnsembleVoice;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Sub-resource for managing voices within an ensemble. Each voice represents an instrument slot
 * (e.g. "1. Trumpet", "Tuba") with a weight and required flag for coverage scoring.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface EnsembleVoicesResource {

    /**
     * Lists all voices for the parent ensemble, ordered by label.
     *
     * @return all voices belonging to this ensemble
     */
    @GET
    List<EnsembleVoice> listAll();

    /**
     * Adds a new voice to the ensemble.
     *
     * @param voice the voice data
     * @return the created voice with generated ID
     */
    @POST
    EnsembleVoice add(CreateEnsembleVoice voice);

    /**
     * Loads a specific voice by its ID.
     *
     * @param voiceId the ID of the voice
     * @return the voice with its options
     */
    @GET
    @Path("{voiceId}")
    EnsembleVoice get(@PathParam("voiceId") String voiceId);

    /**
     * Updates an existing voice's label, weight, and required flag.
     *
     * @param voiceId the ID of the voice to update
     * @param voice the updated voice data
     */
    @PUT
    @Path("{voiceId}")
    void update(@PathParam("voiceId") String voiceId, EnsembleVoice voice);

    /**
     * Deletes a voice and all its options.
     *
     * @param voiceId the ID of the voice to delete
     */
    @DELETE
    @Path("{voiceId}")
    void delete(@PathParam("voiceId") String voiceId);

    /**
     * Provides access to the options sub-resource for a specific voice.
     *
     * @param voiceId the ID of the voice
     * @return the voice options resource
     */
    @Path("{voiceId}/options")
    VoiceOptionsResource options(@PathParam("voiceId") String voiceId);
}
