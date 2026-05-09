package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.collections.CollectionSheet;
import de.halbmann.sam.api.entity.collections.CreateCollectionSheet;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * Sub-resource for managing the sheet entries within a booklet. This is the legacy sheet-only
 * variant; new collection types use {@link CollectionItemsResource} which supports both sheet
 * references and free-text blocks.
 */
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
