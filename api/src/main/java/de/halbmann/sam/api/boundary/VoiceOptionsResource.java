package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.instruments.CreateVoiceOption;
import de.halbmann.sam.api.entity.instruments.VoiceOption;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Sub-resource for managing instrument options within an ensemble voice. Each option maps a
 * canonical instrument to a voice with a priority type and scoring factor.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface VoiceOptionsResource {

    /**
     * Lists all instrument options for the parent voice.
     *
     * @return all options belonging to this voice
     */
    @GET
    List<VoiceOption> listAll();

    /**
     * Adds a new instrument option to the voice.
     *
     * @param option the option data (instrument reference, type, factor)
     * @return the created option with generated ID
     */
    @POST
    VoiceOption add(CreateVoiceOption option);

    /**
     * Updates an existing option's type, factor, or instrument reference.
     *
     * @param optionId the ID of the option to update
     * @param option the updated option data
     */
    @PUT
    @Path("{optionId}")
    void update(@PathParam("optionId") String optionId, VoiceOption option);

    /**
     * Deletes an instrument option.
     *
     * @param optionId the ID of the option to delete
     */
    @DELETE
    @Path("{optionId}")
    void delete(@PathParam("optionId") String optionId);
}
