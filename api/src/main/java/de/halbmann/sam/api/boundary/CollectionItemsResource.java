package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.assistant.DraftTextResult;
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

    /**
     * Returns a paginated list of items in the collection.
     *
     * @param paginationRequest pagination parameters
     * @param myPartsOnly       when {@code true}, only sheet items that match the calling musician's
     *                          instrument assignments are returned
     * @return paginated list of collection items
     */
    @GET
    PaginatedResponse<CollectionItem> listAll(
            @BeanParam PaginationRequest paginationRequest,
            @QueryParam("myPartsOnly") @DefaultValue("false") boolean myPartsOnly);

    /**
     * Adds a new item (sheet reference or free-text block) to the collection.
     *
     * @param createCollectionItem the item to add
     */
    @POST
    void addItem(CreateCollectionItem createCollectionItem);

    /**
     * Updates an existing collection item.
     *
     * @param itemId          the item ID
     * @param collectionItem  the updated item data
     */
    @PUT
    @Path("{itemId}")
    void updateItem(@PathParam("itemId") String itemId, CollectionItem collectionItem);

    /**
     * Removes an item from the collection.
     *
     * @param itemId the item ID
     */
    @DELETE
    @Path("{itemId}")
    void removeItem(@PathParam("itemId") String itemId);

    /**
     * Uploads a file attachment (e.g. a programme note PDF) for a text block item.
     *
     * @param itemId  the item ID
     * @param request the multipart upload including file and attachment type
     * @return the created attachment record
     */
    @POST
    @Path("{itemId}/attachment")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    Attachment uploadAttachment(@PathParam("itemId") String itemId, @BeanParam FileUploadRequest request);

    /**
     * Removes the attachment from the given collection item.
     *
     * @param itemId the item ID
     */
    @DELETE
    @Path("{itemId}/attachment")
    void removeAttachment(@PathParam("itemId") String itemId);

    /**
     * Replaces the display order of all items in the collection with the given ordered list of IDs.
     *
     * @param orderedIds IDs of all items in the desired order
     */
    @PUT
    @Path("order")
    void reorderItems(List<String> orderedIds);

    /**
     * AI setlist assistant: drafts spoken introduction text for a TEXT item, based on the
     * neighboring piece's metadata. The Dirigent/announcer reviews and edits before use.
     *
     * @param itemId   the TEXT item ID
     * @param language ISO 639-1 language code to draft the text in (e.g. "en", "de"); defaults to "en"
     * @return the drafted text
     */
    @POST
    @Path("{itemId}/ai/draft-text")
    DraftTextResult draftText(
            @PathParam("itemId") String itemId, @QueryParam("language") @DefaultValue("en") String language);
}
