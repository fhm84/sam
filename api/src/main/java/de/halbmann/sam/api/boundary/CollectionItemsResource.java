package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.collections.CollectionItem;
import de.halbmann.sam.api.entity.collections.CreateCollectionItem;
import de.halbmann.sam.api.entity.documents.Attachment;
import de.halbmann.sam.api.entity.documents.FileUploadRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Sub-resource for managing items within a sheet collection. Items are either sheet references
 * ({@code SHEET}) or free-text blocks ({@code TEXT}), both handled through this single interface
 * using the {@link de.halbmann.sam.api.entity.collections.CreateCollectionItem} discriminated DTO.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface CollectionItemsResource {

    @GET
    PaginatedResponse<CollectionItem> listAll(
            @BeanParam PaginationRequest paginationRequest,
            @QueryParam("myPartsOnly") @DefaultValue("false") boolean myPartsOnly);

    @POST
    void addItem(CreateCollectionItem createCollectionItem);

    @PUT
    @Path("{itemId}")
    void updateItem(@PathParam("itemId") String itemId, CollectionItem collectionItem);

    @DELETE
    @Path("{itemId}")
    void removeItem(@PathParam("itemId") String itemId);

    @POST
    @Path("{itemId}/attachment")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    Attachment uploadAttachment(@PathParam("itemId") String itemId, @BeanParam FileUploadRequest request);

    @DELETE
    @Path("{itemId}/attachment")
    void removeAttachment(@PathParam("itemId") String itemId);

    @PUT
    @Path("order")
    void reorderItems(List<String> orderedIds);
}
