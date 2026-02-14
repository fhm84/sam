package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.CreateInstrument;
import de.halbmann.sam.api.entity.Instrument;
import de.halbmann.sam.api.entity.InstrumentFilterRequest;
import de.halbmann.sam.api.entity.PaginatedResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("instruments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface InstrumentsResource {

    @GET
    PaginatedResponse<Instrument> findInstruments(final @BeanParam InstrumentFilterRequest filterRequest);

    @POST
    Instrument add(final CreateInstrument instrument);

    @GET
    @Path("{instrumentId}")
    Instrument load(final @PathParam("instrumentId") String instrumentId);

    @PUT
    @Path("{instrumentId}")
    void update(final @PathParam("instrumentId") String instrumentId, final Instrument instrument);

    @DELETE
    @Path("{instrumentId}")
    void delete(final @PathParam("instrumentId") String instrumentId);
}
