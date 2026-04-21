package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.instruments.CreateInstrument;
import de.halbmann.sam.api.entity.instruments.Instrument;
import de.halbmann.sam.api.entity.instruments.InstrumentFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("instruments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface InstrumentsResource {

    @GET
    PaginatedResponse<Instrument> findInstruments(@BeanParam InstrumentFilterRequest filterRequest);

    @POST
    Instrument add(CreateInstrument instrument);

    @GET
    @Path("{instrumentId}")
    Instrument load(@PathParam("instrumentId") String instrumentId);

    @PUT
    @Path("{instrumentId}")
    void update(@PathParam("instrumentId") String instrumentId, Instrument instrument);

    @DELETE
    @Path("{instrumentId}")
    void delete(@PathParam("instrumentId") String instrumentId);
}
