package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.CollectionSheetsResource;
import de.halbmann.sam.api.entity.CollectionSheet;
import de.halbmann.sam.api.entity.CreateCollectionSheet;
import de.halbmann.sam.api.entity.PaginatedResponse;
import de.halbmann.sam.api.entity.PaginationRequest;
import de.halbmann.sam.business.controller.SheetCollectionService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.PathParam;

@RequestScoped
public class CollectionSheetsResourceImpl implements CollectionSheetsResource {

    @PathParam("collectionId")
    String collectionId;

    @Inject
    SheetCollectionService service;

    @Override
    public PaginatedResponse<CollectionSheet> listAll(final PaginationRequest paginationRequest) {
        return service.listSheets(collectionId, paginationRequest);
    }

    @Override
    public void addSheet(final CreateCollectionSheet createCollectionSheet) {
        service.addSheet(collectionId, createCollectionSheet);
    }

    @Override
    public void updateSheet(final String sheetId, final CollectionSheet collectionSheet) {
        service.updateSheet(collectionId, sheetId, collectionSheet);
    }

    @Override
    public void removeSheet(final String sheetId) {
        service.removeSheet(collectionId, sheetId);
    }
}
