package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.CollectionSheetsResource;
import de.halbmann.sam.api.boundary.SheetCollectionsResource;
import de.halbmann.sam.api.entity.ExportFormat;
import de.halbmann.sam.api.entity.PaginatedResponse;
import de.halbmann.sam.api.entity.SheetCollection;
import de.halbmann.sam.api.entity.SheetCollectionFilterRequest;
import de.halbmann.sam.business.controller.CollectionTocService;
import de.halbmann.sam.business.controller.ExportResult;
import de.halbmann.sam.business.controller.SheetCollectionService;
import de.halbmann.sam.business.controller.SheetExportService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@RequestScoped
public class SheetCollectionsResourceImpl implements SheetCollectionsResource {

    @Context
    ResourceContext resourceContext;

    @Inject
    SheetCollectionService service;

    @Inject
    CollectionTocService tocService;

    @Inject
    SheetExportService sheetExportService;

    @Override
    public PaginatedResponse<SheetCollection> findSheetCollections(final SheetCollectionFilterRequest filterRequest) {
        return service.findCollections(filterRequest);
    }

    @Override
    public SheetCollection add(final SheetCollection sheetCollection) {
        return service.add(sheetCollection);
    }

    @Override
    public SheetCollection load(final String collectionId) {
        return service.load(collectionId);
    }

    @Override
    public void update(final String collectionId, final SheetCollection sheetCollection) {
        service.update(collectionId, sheetCollection);
    }

    @Override
    public void delete(final String collectionId) {
        service.delete(collectionId);
    }

    @Override
    public Response generateToc(final String collectionId) {
        byte[] pdf = tocService.generateToc(collectionId);
        String filename = "toc-" + collectionId + ".pdf";
        return Response.ok(pdf)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Length", pdf.length)
                .build();
    }

    @Override
    public Response export(final String collectionId, final ExportFormat format) {
        ExportResult result = sheetExportService.exportCollection(collectionId, format);
        return Response.ok((StreamingOutput) result.body()::write)
                .header("Content-Disposition", "attachment; filename=\"" + result.filename() + "\"")
                .type(result.contentType())
                .build();
    }

    @Override
    public CollectionSheetsResource sheets(final String collectionId) {
        return resourceContext.getResource(CollectionSheetsResourceImpl.class);
    }
}
