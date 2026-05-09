package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.collections.SheetCollectionFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.sheets.ExportFormat;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST API for managing sheet music collections (setlists, songbooks, etc.). Supports CRUD
 * operations, PDF table-of-contents generation, GEMA setlist export, and bulk ZIP/JSON/CSV export.
 * The {@code items} sub-resource manages the ordered entries within each collection.
 */
@Path("sheet-collections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SheetCollectionsResource {

    @GET
    PaginatedResponse<SheetCollection> findSheetCollections(@BeanParam SheetCollectionFilterRequest filterRequest);

    @POST
    SheetCollection add(SheetCollection sheetCollection);

    @GET
    @Path("{collectionId}")
    SheetCollection load(@PathParam("collectionId") String collectionId);

    @PUT
    @Path("{collectionId}")
    void update(@PathParam("collectionId") String collectionId, SheetCollection sheetCollection);

    @DELETE
    @Path("{collectionId}")
    void delete(@PathParam("collectionId") String collectionId);

    @GET
    @Path("{collectionId}/toc")
    @Produces("application/pdf")
    Response generateToc(
            @PathParam("collectionId") String collectionId,
            @QueryParam("includeTextItems") @DefaultValue("true") boolean includeTextItems);

    @GET
    @Path("{collectionId}/gema-setlist")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    Response generateGemaSetlist(@PathParam("collectionId") String collectionId);

    /**
     * Exports a collection as a downloadable file.
     *
     * <ul>
     *   <li>{@code ZIP} (default) — collection.json + per-sheet folders with metadata and attachments</li>
     *   <li>{@code JSON} — collection metadata (including sheet list) as a JSON download</li>
     *   <li>{@code CSV} — one row per sheet with metadata fields as a CSV download</li>
     * </ul>
     *
     * @param collectionId          the ID of the collection to export
     * @param format                the export format (ZIP, JSON, CSV); defaults to ZIP
     * @param includeTextItems      whether to include free-text items; defaults to true (ignored for ZIP)
     * @param includeTextAttachments whether to include programme-note attachments from text blocks in
     *                              the ZIP {@code Programmnotizen/} folder; defaults to false (ZIP only)
     * @return the exported file as a download response
     */
    @GET
    @Path("{collectionId}/export")
    @Produces(MediaType.WILDCARD)
    Response export(
            @PathParam("collectionId") String collectionId,
            @QueryParam("format") @DefaultValue("ZIP") ExportFormat format,
            @QueryParam("includeTextItems") @DefaultValue("true") boolean includeTextItems,
            @QueryParam("includeTextAttachments") @DefaultValue("false") boolean includeTextAttachments);

    @Path("{collectionId}/items")
    CollectionItemsResource items(@PathParam("collectionId") String collectionId);
}
