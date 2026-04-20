package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.CollectionSheetsResource;
import de.halbmann.sam.api.boundary.SheetCollectionsResource;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.collections.SheetCollectionFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.sheets.ExportFormat;
import de.halbmann.sam.business.collections.controller.CollectionTocService;
import de.halbmann.sam.business.collections.controller.GemaSetlistService;
import de.halbmann.sam.business.collections.controller.SheetCollectionService;
import de.halbmann.sam.business.sheets.controller.ExportResult;
import de.halbmann.sam.business.sheets.controller.SheetExportService;
import de.halbmann.sam.security.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

@Authenticated
@RequestScoped
public class SheetCollectionsResourceImpl implements SheetCollectionsResource {

    @Context
    ResourceContext resourceContext;

    @Inject
    SheetCollectionService service;

    @Inject
    CollectionTocService tocService;

    @Inject
    GemaSetlistService gemaSetlistService;

    @Inject
    SheetExportService sheetExportService;

    @Override
    public PaginatedResponse<SheetCollection> findSheetCollections(final SheetCollectionFilterRequest filterRequest) {
        return service.findCollections(filterRequest);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public SheetCollection add(final SheetCollection sheetCollection) {
        return service.add(sheetCollection);
    }

    @Override
    public SheetCollection load(final String collectionId) {
        return service.load(collectionId);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void update(final String collectionId, final SheetCollection sheetCollection) {
        service.update(collectionId, sheetCollection);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
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
    public Response generateGemaSetlist(final String collectionId) {
        ExportResult result = gemaSetlistService.generateGemaSetlist(collectionId);
        return Response.ok((StreamingOutput) result.body()::write)
                .header("Content-Disposition", "attachment; filename=\"" + result.filename() + "\"")
                .type(result.contentType())
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
