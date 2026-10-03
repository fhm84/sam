package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.SharesResource;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shares.CreateShareRequest;
import de.halbmann.sam.api.entity.shares.ShareResponse;
import de.halbmann.sam.business.shares.controller.ShareService;
import de.halbmann.sam.security.CurrentUserService;
import de.halbmann.sam.security.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.UUID;

/**
 * Authenticated implementation of {@link SharesResource}.
 * All operations are automatically scoped to the calling user via {@link CurrentUserService}.
 * Creating a share publishes the resource to anyone holding the link, so it requires a write role;
 * listing and revoking stay open because they only ever touch the caller's own shares.
 */
@Authenticated
@RequestScoped
public class SharesResourceImpl implements SharesResource {

    @Inject
    ShareService shareService;

    @Inject
    CurrentUserService currentUserService;

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
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
