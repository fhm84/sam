package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.sheets.Booklet;
import de.halbmann.sam.api.entity.sheets.BookletFilterRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * REST API for managing booklets. A booklet is a physical binder or printed compilation that
 * groups sheet music entries by position. Sheet entries within a booklet are managed via the
 * {@code sheets} sub-resource ({@link CollectionSheetsResource}).
 */
@Path("booklets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BookletsResource {

    @GET
    PaginatedResponse<Booklet> findBooklets(@BeanParam BookletFilterRequest filterRequest);

    @POST
    Booklet add(Booklet booklet);

    @GET
    @Path("{bookletId}")
    Booklet load(@PathParam("bookletId") String bookletId);

    @PUT
    @Path("{bookletId}")
    void update(@PathParam("bookletId") String bookletId, Booklet booklet);

    @DELETE
    @Path("{bookletId}")
    void delete(@PathParam("bookletId") String bookletId);

    @Path("{bookletId}/sheets")
    CollectionSheetsResource sheets(@PathParam("bookletId") String bookletId);
}
