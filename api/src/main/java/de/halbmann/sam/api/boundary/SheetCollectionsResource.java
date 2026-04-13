package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.collections.SheetCollectionFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.sheets.ExportFormat;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("sheet-collections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface SheetCollectionsResource {

    @GET
    PaginatedResponse<SheetCollection> findSheetCollections(
            final @BeanParam SheetCollectionFilterRequest filterRequest);

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

    @GET
    @Path("{collectionId}/toc")
    @Produces("application/pdf")
    Response generateToc(final @PathParam("collectionId") String collectionId);

    /**
     * Exports a collection as a downloadable file.
     *
     * <ul>
     *   <li>{@code ZIP} (default) — collection.json + per-sheet folders with metadata and attachments</li>
     *   <li>{@code JSON} — collection metadata (including sheet list) as a JSON download</li>
     *   <li>{@code CSV} — one row per sheet with metadata fields as a CSV download</li>
     * </ul>
     *
     * @param collectionId the ID of the collection to export
     * @param format       the export format (ZIP, JSON, CSV); defaults to ZIP
     * @return the exported file as a download response
     */
    @GET
    @Path("{collectionId}/export")
    @Produces(MediaType.WILDCARD)
    Response export(
            final @PathParam("collectionId") String collectionId,
            final @QueryParam("format") @DefaultValue("ZIP") ExportFormat format);

    @Path("{collectionId}/sheets")
    CollectionSheetsResource sheets(final @PathParam("collectionId") String collectionId);
}
