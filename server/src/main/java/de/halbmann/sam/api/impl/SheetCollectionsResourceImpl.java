package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.CollectionItemsResource;
import de.halbmann.sam.api.boundary.SheetCollectionsResource;
import de.halbmann.sam.api.entity.assistant.SetlistSuggestions;
import de.halbmann.sam.api.entity.assistant.SuggestSetlistItemsRequest;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.collections.SheetCollectionFilterRequest;
import de.halbmann.sam.api.entity.eventlog.EventType;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.sheets.ExportFormat;
import de.halbmann.sam.assistant.controller.SetlistAssistantService;
import de.halbmann.sam.business.collections.controller.CollectionTocService;
import de.halbmann.sam.business.collections.controller.GemaSetlistService;
import de.halbmann.sam.business.collections.controller.SheetCollectionService;
import de.halbmann.sam.business.eventlog.controller.EventLogService;
import de.halbmann.sam.business.sheets.controller.ExportResult;
import de.halbmann.sam.business.sheets.controller.SheetExportService;
import de.halbmann.sam.core.exception.ValidationException;
import de.halbmann.sam.security.CurrentUserService;
import de.halbmann.sam.security.Roles;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.util.Map;
import java.util.UUID;

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

    @Inject
    EventLogService eventLogService;

    @Inject
    SetlistAssistantService setlistAssistantService;

    @Inject
    CurrentUserService currentUserService;

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
    public Response generateToc(final String collectionId, final boolean includeTextItems) {
        byte[] pdf = tocService.generateToc(collectionId, includeTextItems);
        String filename = "toc-" + collectionId + ".pdf";
        eventLogService.log(
                EventType.COLLECTION_TOC_GENERATED,
                "collection",
                UUID.fromString(collectionId),
                Map.of("filename", filename));
        return Response.ok(pdf)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Length", pdf.length)
                .build();
    }

    @Override
    public Response generateGemaSetlist(final String collectionId) {
        ExportResult result = gemaSetlistService.generateGemaSetlist(collectionId);
        eventLogService.log(
                EventType.GEMA_SETLIST_GENERATED,
                "collection",
                UUID.fromString(collectionId),
                Map.of("filename", result.filename()));
        return Response.ok((StreamingOutput) result.body()::write)
                .header("Content-Disposition", "attachment; filename=\"" + result.filename() + "\"")
                .type(result.contentType())
                .build();
    }

    @Override
    public Response export(
            final String collectionId,
            final ExportFormat format,
            final boolean includeTextItems,
            final boolean includeTextAttachments) {
        ExportResult result =
                sheetExportService.exportCollection(collectionId, format, includeTextItems, includeTextAttachments);
        eventLogService.log(
                EventType.COLLECTION_EXPORT,
                "collection",
                UUID.fromString(collectionId),
                Map.of("format", format.name(), "filename", result.filename()));
        return Response.ok((StreamingOutput) result.body()::write)
                .header("Content-Disposition", "attachment; filename=\"" + result.filename() + "\"")
                .type(result.contentType())
                .build();
    }

    @Override
    public CollectionItemsResource items(final String collectionId) {
        return resourceContext.getResource(CollectionItemsResourceImpl.class);
    }

    @Override
    @RolesAllowed({Roles.MUSIC_LIBRARIAN, Roles.ADMIN})
    public SetlistSuggestions suggestItems(final String collectionId, final SuggestSetlistItemsRequest request) {
        SheetCollection collection = service.load(collectionId);
        if (collection.getEnsembleId() == null) {
            throw new ValidationException(
                    "This collection has no ensemble set; assign one before using the assistant.");
        }
        if (!currentUserService.canAccessEnsemble(collection.getEnsembleId())) {
            throw new ForbiddenException();
        }

        SetlistAssistantService.SuggestionOutcome outcome =
                setlistAssistantService.suggestItems(collectionId, collection.getEnsembleId(), request.getGoal());

        eventLogService.log(
                EventType.SETLIST_AI_SUGGESTION_GENERATED,
                "collection",
                UUID.fromString(collectionId),
                Map.of(
                        "suggestionCount",
                        outcome.suggestions().getItems().size(),
                        "inputTokens",
                        outcome.tokenUsage() != null ? outcome.tokenUsage().inputTokenCount() : 0,
                        "outputTokens",
                        outcome.tokenUsage() != null ? outcome.tokenUsage().outputTokenCount() : 0));

        return outcome.suggestions();
    }
}
