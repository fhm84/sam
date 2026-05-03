package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.SharesResource;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shares.CreateShareRequest;
import de.halbmann.sam.api.entity.shares.ShareResponse;
import de.halbmann.sam.business.shares.controller.ShareService;
import de.halbmann.sam.security.CurrentUserService;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.UUID;

/**
 * Authenticated implementation of {@link SharesResource}.
 * All operations are automatically scoped to the calling user via {@link CurrentUserService}.
 */
@Authenticated
@RequestScoped
public class SharesResourceImpl implements SharesResource {

    @Inject
    ShareService shareService;

    @Inject
    CurrentUserService currentUserService;

    @Override
    public ShareResponse create(CreateShareRequest request) {
        return shareService.create(request, currentUserService.getUserId());
    }

    @Override
    public PaginatedResponse<ShareResponse> list() {
        return shareService.listByCreator(currentUserService.getUserId());
    }

    @Override
    public void revoke(UUID id) {
        shareService.revoke(id, currentUserService.getUserId());
    }
}
