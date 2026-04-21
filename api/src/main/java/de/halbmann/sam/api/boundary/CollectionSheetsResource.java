package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.collections.CollectionSheet;
import de.halbmann.sam.api.entity.collections.CreateCollectionSheet;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface CollectionSheetsResource {

    @GET
    PaginatedResponse<CollectionSheet> listAll(@BeanParam PaginationRequest paginationRequest);

    @POST
    void addSheet(CreateCollectionSheet createCollectionSheet);

    @PUT
    @Path("{sheetId}")
    void updateSheet(@PathParam("sheetId") String sheetId, CollectionSheet collectionSheet);

    @DELETE
    @Path("{sheetId}")
    void removeSheet(@PathParam("sheetId") String sheetId);
}
