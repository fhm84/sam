package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.PaginatedResponse;
import de.halbmann.sam.api.entity.SheetFilterRequest;
import de.halbmann.sam.api.entity.SheetMusic;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("sheets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SheetsResource {

    @GET
    PaginatedResponse<SheetMusic> findSheets(final @BeanParam SheetFilterRequest filterRequest);

    @POST
    SheetMusic add(final SheetMusic sheetMusic);

    @GET
    @Path("{sheetId}")
    SheetMusic load(final @PathParam("sheetId") String sheetId);

    @PUT
    @Path("{sheetId}")
    void update(final @PathParam("sheetId") String sheetId, final SheetMusic sheetMusic);

    @DELETE
    @Path("{sheetId}")
    void delete(final @PathParam("sheetId") String sheetId);

    @Path("{sheetId}/instrumentations")
    InstrumentationsResource instrumentations(final @PathParam("sheetId") String sheetId);

}
