package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.instruments.CreateInstrument;
import de.halbmann.sam.api.entity.instruments.Instrument;
import de.halbmann.sam.api.entity.instruments.InstrumentFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * REST API for managing canonical instrument definitions. Instruments are the shared reference
 * data used by instrumentations and ensemble voices — changes here affect all referencing records.
 */
@Path("instruments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface InstrumentsResource {

    /**
     * Returns a paginated list of instruments matching the given filter criteria.
     *
     * @param filterRequest name and transposition filters, plus pagination parameters
     * @return paginated list of matching instruments
     */
    @GET
    PaginatedResponse<Instrument> findInstruments(@BeanParam InstrumentFilterRequest filterRequest);

    /**
     * Creates a new instrument definition.
     *
     * @param instrument the instrument to create
     * @return the persisted instrument including its generated ID
     */
    @POST
    Instrument add(CreateInstrument instrument);

    /**
     * Loads a single instrument by its ID.
     *
     * @param instrumentId the instrument ID
     * @return the instrument
     */
    @GET
    @Path("{instrumentId}")
    Instrument load(@PathParam("instrumentId") String instrumentId);

    /**
     * Updates an existing instrument definition.
     *
     * @param instrumentId the instrument ID
     * @param instrument   the updated instrument data
     */
    @PUT
    @Path("{instrumentId}")
    void update(@PathParam("instrumentId") String instrumentId, Instrument instrument);

    /**
     * Deletes an instrument by its ID.
     *
     * @param instrumentId the instrument ID
     */
    @DELETE
    @Path("{instrumentId}")
    void delete(@PathParam("instrumentId") String instrumentId);
}
