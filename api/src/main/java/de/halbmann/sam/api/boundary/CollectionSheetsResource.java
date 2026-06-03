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

    /**
     * Returns a paginated list of sheet entries in the booklet.
     *
     * @param paginationRequest pagination parameters
     * @return paginated list of booklet sheet entries
     */
    @GET
    PaginatedResponse<CollectionSheet> listAll(@BeanParam PaginationRequest paginationRequest);

    /**
     * Adds a sheet entry to the booklet.
     *
     * @param createCollectionSheet the sheet entry to add
     */
    @POST
    void addSheet(CreateCollectionSheet createCollectionSheet);

    /**
     * Updates a sheet entry within the booklet (e.g. page number or notes).
     *
     * @param sheetId         the sheet entry ID
     * @param collectionSheet the updated sheet entry data
     */
    @PUT
    @Path("{sheetId}")
    void updateSheet(@PathParam("sheetId") String sheetId, CollectionSheet collectionSheet);

    /**
     * Removes a sheet entry from the booklet.
     *
     * @param sheetId the sheet entry ID
     */
    @DELETE
    @Path("{sheetId}")
    void removeSheet(@PathParam("sheetId") String sheetId);
}
