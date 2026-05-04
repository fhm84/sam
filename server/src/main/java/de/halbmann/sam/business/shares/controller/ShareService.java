package de.halbmann.sam.business.shares.controller;

import de.halbmann.sam.api.entity.eventlog.EventType;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shares.*;
import de.halbmann.sam.business.collections.boundary.SheetCollectionRepository;
import de.halbmann.sam.business.collections.entity.CollectionSheetEntity;
import de.halbmann.sam.business.collections.entity.SheetCollectionEntity;
import de.halbmann.sam.business.documents.controller.DocumentsService;
import de.halbmann.sam.business.eventlog.controller.EventLogService;
import de.halbmann.sam.business.shares.boundary.ShareRepository;
import de.halbmann.sam.business.shares.entity.ShareEntity;
import de.halbmann.sam.business.sheets.boundary.InstrumentationRepository;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.entity.InstrumentationEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for share-link lifecycle: creation, listing, revocation, and token validation.
 *
 * <p>Also responsible for building the public-facing {@link PublicShareInfo} response and for
 * resolving human-readable resource labels used in the authenticated shares overview.
 */
@ApplicationScoped
public class ShareService {

    @Inject
    ShareRepository shareRepository;

    @Inject
    ShareMapper shareMapper;

    @Inject
    InstrumentationRepository instrumentationRepository;

    @Inject
    SheetRepository sheetRepository;

    @Inject
    SheetCollectionRepository collectionRepository;

    @Inject
    DocumentsService documentsService;

    @Inject
    EventLogService eventLogService;

    @Transactional
    public ShareResponse create(CreateShareRequest request, String creatorUserId) {
        ShareEntity entity = new ShareEntity();
        entity.setCreatorUserId(creatorUserId);
        entity.setResourceType(request.getResourceType());
        entity.setResourceId(request.getResourceId());
        entity.setExpiresAt(request.getExpiresAt());
        shareRepository.persistAndFlush(entity);

        eventLogService.log(
                EventType.SHARE_CREATED,
                request.getResourceType().name().toLowerCase(),
                request.getResourceId(),
                Map.of("shareId", entity.getId().toString()),
                entity.getId());

        return shareMapper.toDto(entity);
    }

    @Transactional
    public void revoke(UUID id, String requestingUserId) {
        ShareEntity entity = shareRepository.findById(id);
        if (entity == null || !entity.getCreatorUserId().equals(requestingUserId)) {
            throw new EntityNotFoundException("share", id);
        }
        entity.setRevokedAt(OffsetDateTime.now());

        eventLogService.log(
                EventType.SHARE_REVOKED,
                entity.getResourceType().name().toLowerCase(),
                entity.getResourceId(),
                Map.of("shareId", id.toString()),
                id);
    }

    public PaginatedResponse<ShareResponse> listByCreator(String userId) {
        List<ShareResponse> items = shareRepository.findByCreator(userId).stream()
                .map(entity -> {
                    ShareResponse dto = shareMapper.toDto(entity);
                    dto.setResourceLabel(resolveResourceLabel(entity));
                    return dto;
                })
                .toList();
        PaginatedResponse<ShareResponse> response = new PaginatedResponse<>();
        response.setData(items);
        response.setPage(0);
        response.setSize(items.size());
        response.setTotalCount(items.size());
        return response;
    }

    private String resolveResourceLabel(ShareEntity entity) {
        if (entity.getResourceType() == ShareType.INSTRUMENTATION) {
            InstrumentationEntity instr = instrumentationRepository.findById(entity.getResourceId());
            if (instr == null) return null;
            String name = instr.getInstrument() != null ? instr.getInstrument().getName() : "?";
            return instr.getPartLabel() != null ? name + " " + instr.getPartLabel() : name;
        } else if (entity.getResourceType() == ShareType.COLLECTION) {
            SheetCollectionEntity col = collectionRepository.findById(entity.getResourceId());
            return col != null ? col.getName() : null;
        } else if (entity.getResourceType() == ShareType.SHEET) {
            SheetMusicEntity sheet = sheetRepository.findById(entity.getResourceId());
            return sheet != null ? sheet.getTitle() : null;
        }
        return null;
    }

    /** Returns the share entity if the token is valid, otherwise throws 404. */
    public ShareEntity validateShare(UUID token) {
        return shareRepository.findValid(token).orElseThrow(() -> new EntityNotFoundException("share", token));
    }

    /** Builds the public metadata response for a share token. Throws 404 if invalid. */
    @Transactional
    public PublicShareInfo getPublicInfo(UUID token) {
        ShareEntity share =
                shareRepository.findByIdOptional(token).orElseThrow(() -> new EntityNotFoundException("share", token));

        PublicShareInfo info = new PublicShareInfo();
        info.setTokenId(share.getId());
        info.setType(share.getResourceType());
        info.setExpiresAt(share.getExpiresAt());
        info.setExpired(share.isExpired() || share.isRevoked());

        if (share.getResourceType() == ShareType.INSTRUMENTATION) {
            buildInstrumentationInfo(share.getResourceId(), info);
        } else if (share.getResourceType() == ShareType.COLLECTION) {
            buildCollectionInfo(share.getResourceId(), info);
        } else if (share.getResourceType() == ShareType.SHEET) {
            buildSheetInfo(share.getResourceId(), info);
        }

        return info;
    }

    private void buildInstrumentationInfo(UUID instrumentationId, PublicShareInfo info) {
        InstrumentationEntity instr = instrumentationRepository.findById(instrumentationId);
        if (instr == null) {
            return;
        }
        SheetMusicEntity sheet = instr.getSheet();
        info.setSheetTitle(sheet != null ? sheet.getTitle() : null);
        info.setComposerName(
                sheet != null && sheet.getComposer() != null
                        ? sheet.getComposer().getName()
                        : null);
        info.setInstrumentName(
                instr.getInstrument() != null ? instr.getInstrument().getName() : null);
        info.setPartLabel(instr.getPartLabel());
        info.setAttachments(documentsService.loadAttachmentsByInstrumentation(instrumentationId.toString()));
    }

    private void buildCollectionInfo(UUID collectionId, PublicShareInfo info) {
        SheetCollectionEntity collection = collectionRepository.findById(collectionId);
        if (collection == null) {
            return;
        }
        info.setCollectionName(collection.getName());
        info.setCollectionType(collection.getType());
        List<PublicShareCollectionItem> sheetItems = collection.getSheets().stream()
                .map(CollectionSheetEntity::getSheet)
                .map(this::toCollectionItem)
                .toList();
        info.setSheets(sheetItems);
    }

    private PublicShareCollectionItem toCollectionItem(SheetMusicEntity sheet) {
        PublicShareCollectionItem item = new PublicShareCollectionItem();
        item.setSheetId(sheet.getId());
        item.setTitle(sheet.getTitle());
        item.setSubtitle(sheet.getSubtitle());
        item.setComposerName(sheet.getComposer() != null ? sheet.getComposer().getName() : null);
        item.setInstrumentations(sheet.getInstrumentations().stream()
                .map(this::toInstrumentationItem)
                .toList());
        return item;
    }

    private PublicShareInstrumentationItem toInstrumentationItem(InstrumentationEntity instr) {
        PublicShareInstrumentationItem item = new PublicShareInstrumentationItem();
        item.setId(instr.getId());
        item.setInstrumentName(
                instr.getInstrument() != null ? instr.getInstrument().getName() : null);
        item.setPartLabel(instr.getPartLabel());
        item.setHasAttachments(
                instr.getAttachments() != null && !instr.getAttachments().isEmpty());
        return item;
    }

    private void buildSheetInfo(UUID sheetId, PublicShareInfo info) {
        SheetMusicEntity sheet = sheetRepository.findById(sheetId);
        if (sheet == null) {
            return;
        }
        info.setSheetTitle(sheet.getTitle());
        info.setComposerName(sheet.getComposer() != null ? sheet.getComposer().getName() : null);
        List<PublicShareInstrumentationItem> items = sheet.getInstrumentations().stream()
                .sorted(Comparator.comparing(
                                (InstrumentationEntity i) -> i.getInstrument().getName())
                        .thenComparing(
                                i -> Optional.ofNullable(i.getPartLabel()).orElse("")))
                .map(this::toInstrumentationItem)
                .toList();
        info.setInstrumentations(items);
        info.setAttachments(documentsService.loadAttachmentsBySheet(sheetId.toString()));
    }

    /**
     * Returns true if the instrumentation belongs to the sheet, false otherwise.
     * Throws 404 if the sheet itself does not exist.
     */
    @Transactional
    public boolean isInstrumentationInSheet(UUID sheetId, UUID instrumentationId) {
        SheetMusicEntity sheet = sheetRepository.findById(sheetId);
        if (sheet == null) {
            throw new EntityNotFoundException("sheet", sheetId);
        }
        return sheet.getInstrumentations().stream().anyMatch(i -> i.getId().equals(instrumentationId));
    }

    /**
     * Returns true if the attachment belongs directly to the sheet (sheet-level, not instrumentation).
     * Throws 404 if the sheet itself does not exist.
     */
    @Transactional
    public boolean isAttachmentOnSheet(UUID sheetId, UUID attachmentId) {
        SheetMusicEntity sheet = sheetRepository.findById(sheetId);
        if (sheet == null) {
            throw new EntityNotFoundException("sheet", sheetId);
        }
        return sheet.getAttachments() != null
                && sheet.getAttachments().stream().anyMatch(a -> a.getId().equals(attachmentId));
    }

    /**
     * Returns true if the instrumentation belongs to any sheet in the collection, false otherwise.
     * Throws 404 if the collection itself does not exist.
     */
    @Transactional
    public boolean isInstrumentationInCollection(UUID collectionId, UUID instrumentationId) {
        SheetCollectionEntity collection = collectionRepository.findById(collectionId);
        if (collection == null) {
            throw new EntityNotFoundException("collection", collectionId);
        }
        return collection.getSheets().stream()
                .map(CollectionSheetEntity::getSheet)
                .flatMap(s -> s.getInstrumentations().stream())
                .anyMatch(i -> i.getId().equals(instrumentationId));
    }

    /** Logs a SHARE_ACCESSED event for download tracking. */
    @Transactional
    public void logAccess(UUID shareId, String resourceType, UUID resourceId) {
        eventLogService.log(
                EventType.SHARE_ACCESSED, resourceType, resourceId, Map.of("shareId", shareId.toString()), shareId);
    }
}
