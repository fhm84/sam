package de.halbmann.sam.api.impl.boundary;

import de.halbmann.sam.api.boundary.CollectionSheetsResource;
import de.halbmann.sam.api.boundary.SheetCollectionsResource;
import de.halbmann.sam.api.entity.PaginatedResponse;
import de.halbmann.sam.api.entity.SheetCollection;
import de.halbmann.sam.api.entity.SheetCollectionFilterRequest;
import de.halbmann.sam.business.controller.SheetCollectionService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

@RequestScoped
public class SheetCollectionsResourceImpl implements SheetCollectionsResource {

    @Context
    ResourceContext resourceContext;

    @Inject
    SheetCollectionService service;

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
    public CollectionSheetsResource sheets(final String collectionId) {
        return resourceContext.getResource(CollectionSheetsResourceImpl.class);
    }
}
