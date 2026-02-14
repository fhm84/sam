package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.Booklet;
import de.halbmann.sam.api.entity.BookletFilterRequest;
import de.halbmann.sam.api.entity.PaginatedResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("booklets")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface BookletsResource {

    @GET
    PaginatedResponse<Booklet> findBooklets(final @BeanParam BookletFilterRequest filterRequest);

    @POST
    Booklet add(final Booklet booklet);

    @GET
    @Path("{bookletId}")
    Booklet load(final @PathParam("bookletId") String bookletId);

    @PUT
    @Path("{bookletId}")
    void update(final @PathParam("bookletId") String bookletId, final Booklet booklet);

    @DELETE
    @Path("{bookletId}")
    void delete(final @PathParam("bookletId") String bookletId);

    @Path("{bookletId}/sheets")
    CollectionSheetsResource sheets(final @PathParam("bookletId") String bookletId);
}
