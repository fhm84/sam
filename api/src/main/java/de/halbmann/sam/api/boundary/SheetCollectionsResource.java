package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.PaginatedResponse;
import de.halbmann.sam.api.entity.SheetCollection;
import de.halbmann.sam.api.entity.SheetFilterRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("sheet-collections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient
public interface SheetCollectionsResource {

    @GET
    PaginatedResponse<SheetCollection> findSheetCollections(final @BeanParam SheetFilterRequest filterRequest);

    @POST
    SheetCollection add(final SheetCollection sheetCollection);

    @GET
    @Path("{collectionId}")
    SheetCollection load(final @PathParam("collectionId") String collectionId);

    @PUT
    @Path("{collectionId}")
    void update(final @PathParam("collectionId") String collectionId, final SheetCollection sheetCollection);

    @DELETE
    @Path("{collectionId}")
    void delete(final @PathParam("collectionId") String collectionId);

    @Path("{collectionId}/sheets")
    CollectionSheetsResource sheets(final @PathParam("collectionId") String collectionId);

}
