package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.MyPartsResource;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.api.entity.sheets.SheetWithMyParts;
import de.halbmann.sam.business.myparts.MyPartsService;
import de.halbmann.sam.security.CurrentUserService;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@Authenticated
@RequestScoped
public class MyPartsResourceImpl implements MyPartsResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    MyPartsService myPartsService;

    @Override
    public PaginatedResponse<SheetWithMyParts> getMyParts(PaginationRequest filter) {
        return myPartsService.getMyParts(currentUserService.getUserId(), filter);
    }
}
