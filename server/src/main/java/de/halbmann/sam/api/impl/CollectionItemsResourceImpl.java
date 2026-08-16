package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.CollectionItemsResource;
import de.halbmann.sam.api.entity.assistant.DraftTextResult;
import de.halbmann.sam.api.entity.collections.CollectionItem;
import de.halbmann.sam.api.entity.collections.CreateCollectionItem;
import de.halbmann.sam.api.entity.documents.Attachment;
import de.halbmann.sam.api.entity.documents.FileUploadRequest;
import de.halbmann.sam.api.entity.eventlog.EventType;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.assistant.controller.ProgrammeTextDraftService;
import de.halbmann.sam.business.collections.controller.SheetCollectionService;
import de.halbmann.sam.business.eventlog.controller.EventLogService;
import de.halbmann.sam.security.CurrentUserService;
import de.halbmann.sam.security.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.PathParam;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Authenticated
@RequestScoped
public class CollectionItemsResourceImpl implements CollectionItemsResource {

    @PathParam("collectionId")
    String collectionId;

    @Inject
    SheetCollectionService service;

    @Inject
    CurrentUserService currentUserService;

    @Inject
    ProgrammeTextDraftService programmeTextDraftService;

    @Inject
    EventLogService eventLogService;

    @Override
    public PaginatedResponse<CollectionItem> listAll(
            final PaginationRequest paginationRequest, final boolean myPartsOnly) {
        return service.listItems(collectionId, paginationRequest, myPartsOnly, currentUserService.getUserId());
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void addItem(final CreateCollectionItem createCollectionItem) {
        service.addItem(collectionId, createCollectionItem);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void updateItem(final String itemId, final CollectionItem collectionItem) {
        service.updateItem(collectionId, itemId, collectionItem);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void removeItem(final String itemId) {
        service.removeItem(collectionId, itemId);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public Attachment uploadAttachment(final String itemId, final FileUploadRequest request) {
        try {
            return service.attachDocument(
                    collectionId,
                    itemId,
                    request.getFile().fileName(),
                    Files.newInputStream(request.getFile().uploadedFile()));
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new InternalServerErrorException("Attachment upload failed", e);
        }
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void removeAttachment(final String itemId) {
        service.removeAttachment(collectionId, itemId);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public void reorderItems(final List<String> orderedIds) {
        service.reorderItems(collectionId, orderedIds);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public DraftTextResult draftText(final String itemId, final String language) {
        ProgrammeTextDraftService.DraftOutcome outcome =
                programmeTextDraftService.draft(collectionId, itemId, language);

        eventLogService.log(
                EventType.SETLIST_AI_TEXT_DRAFTED,
                "collectionItem",
                UUID.fromString(itemId),
                Map.of(
                        "inputTokens",
                        outcome.tokenUsage() != null ? outcome.tokenUsage().inputTokenCount() : 0,
                        "outputTokens",
                        outcome.tokenUsage() != null ? outcome.tokenUsage().outputTokenCount() : 0));

        DraftTextResult result = new DraftTextResult();
        result.setDraftText(outcome.draftText());
        return result;
    }
}
