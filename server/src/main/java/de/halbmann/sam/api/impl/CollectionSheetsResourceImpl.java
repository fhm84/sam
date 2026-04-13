package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.CollectionSheetsResource;
import de.halbmann.sam.api.entity.collections.CollectionSheet;
import de.halbmann.sam.api.entity.collections.CreateCollectionSheet;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.business.collections.controller.SheetCollectionService;
import de.halbmann.sam.security.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.PathParam;

@Authenticated
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
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void addSheet(final CreateCollectionSheet createCollectionSheet) {
        service.addSheet(collectionId, createCollectionSheet);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void updateSheet(final String sheetId, final CollectionSheet collectionSheet) {
        service.updateSheet(collectionId, sheetId, collectionSheet);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void removeSheet(final String sheetId) {
        service.removeSheet(collectionId, sheetId);
    }
}
