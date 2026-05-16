package de.halbmann.sam.business.collections.controller;

import de.halbmann.sam.api.entity.collections.CollectionItem;
import de.halbmann.sam.api.entity.collections.CollectionItemType;
import de.halbmann.sam.api.entity.collections.CreateCollectionItem;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.collections.SheetCollectionFilterRequest;
import de.halbmann.sam.api.entity.documents.Attachment;
import de.halbmann.sam.api.entity.documents.AttachmentType;
import de.halbmann.sam.api.entity.documents.DocumentUpload;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.business.collections.boundary.CollectionItemRepository;
import de.halbmann.sam.business.collections.boundary.SheetCollectionRepository;
import de.halbmann.sam.business.collections.entity.CollectionItemEntity;
import de.halbmann.sam.business.collections.entity.SheetCollectionEntity;
import de.halbmann.sam.business.collections.entity.SheetCollectionItemEntity;
import de.halbmann.sam.business.collections.entity.TextCollectionItemEntity;
import de.halbmann.sam.business.documents.boundary.AttachmentRepository;
import de.halbmann.sam.business.documents.controller.DocumentsService;
import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.ensembles.boundary.EnsembleMembershipRepository;
import de.halbmann.sam.business.ensembles.entity.EnsembleMembershipEntity;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.business.musicians.boundary.MusicianRepository;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.core.entity.PaginatedEntities;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import de.halbmann.sam.core.exception.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class SheetCollectionService {

    @Inject
    SheetCollectionRepository repository;

    @Inject
    CollectionItemRepository collectionItemRepository;

    @Inject
    SheetCollectionMapper mapper;

    @Inject
    CollectionItemMapper collectionItemMapper;

    @Inject
    SheetRepository sheetRepository;

    @Inject
    DocumentsService documentsService;

    @Inject
    AttachmentRepository attachmentRepository;

    @Inject
    MusicianRepository musicianRepository;

    @Inject
    EnsembleMembershipRepository membershipRepository;

    public PaginatedResponse<SheetCollection> findCollections(final SheetCollectionFilterRequest filter) {
        PaginatedEntities<SheetCollectionEntity> result =
                repository.findCollections(filter, filter.getName(), filter.getType());
        PaginatedResponse<SheetCollection> response = new PaginatedResponse<>();
        response.setData(result.data().stream().map(mapper::toDto).toList());
        response.setPage(filter.getPage());
        response.setSize(response.getData().size());
        response.setTotalCount(result.totalCount());
        return response;
    }

    public SheetCollection add(final SheetCollection dto) {
        SheetCollectionEntity entity = mapper.fromDto(dto);
        repository.persistAndFlush(entity);
        return mapper.toDto(entity);
    }

    public SheetCollection load(final String id) {
        SheetCollectionEntity entity = repository
                .findByIdOptional(UUID.fromString(id))
                .orElseThrow(() -> new EntityNotFoundException("SheetCollection", id));
        return mapper.toDto(entity);
    }

    public void update(final String id, final SheetCollection dto) {
        SheetCollectionEntity entity = repository
                .findByIdOptional(UUID.fromString(id))
                .orElseThrow(() -> new EntityNotFoundException("SheetCollection", id));
        mapper.update(entity, dto);
    }

    public void delete(final String id) {
        SheetCollectionEntity entity = repository
                .findByIdOptional(UUID.fromString(id))
                .orElseThrow(() -> new EntityNotFoundException("SheetCollection", id));
        repository.delete(entity);
    }

    public PaginatedResponse<CollectionItem> listItems(
            final String collectionId,
            final PaginationRequest pagination,
            final boolean myPartsOnly,
            final String userId) {
        SheetCollectionEntity collection = repository
                .findByIdOptional(UUID.fromString(collectionId))
                .orElseThrow(() -> new EntityNotFoundException("SheetCollection", collectionId));

        List<CollectionItemEntity> source = collection.getItems();

        if (myPartsOnly && userId != null) {
            Set<String> myInstrumentIds = resolveInstrumentIds(userId);
            source = source.stream()
                    .filter(item -> item instanceof SheetCollectionItemEntity sheetItem
                            && sheetItem.getSheet() != null
                            && sheetItem.getSheet().getInstrumentations().stream()
                                    .anyMatch(i -> i.getInstrument() != null
                                            && myInstrumentIds.contains(
                                                    i.getInstrument().getId())))
                    .toList();
        }

        List<CollectionItem> items =
                source.stream().map(collectionItemMapper::toDto).toList();
        PaginatedResponse<CollectionItem> response = new PaginatedResponse<>();
        response.setData(items);
        response.setPage(0);
        response.setSize(items.size());
        response.setTotalCount((long) items.size());
        return response;
    }

    private Set<String> resolveInstrumentIds(final String userId) {
        return musicianRepository
                .findByUserId(userId)
                .map(musician ->
                        membershipRepository.list("musician = :musician", Map.of("musician", musician)).stream()
                                .map(EnsembleMembershipEntity::getInstrument)
                                .filter(Objects::nonNull)
                                .map(InstrumentEntity::getId)
                                .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    public void addItem(final String collectionId, final CreateCollectionItem createCollectionItem) {
        SheetCollectionEntity collection = repository
                .findByIdOptional(UUID.fromString(collectionId))
                .orElseThrow(() -> new EntityNotFoundException("SheetCollection", collectionId));

        CollectionItemEntity itemEntity;
        if (createCollectionItem.getType() == CollectionItemType.SHEET) {
            if (createCollectionItem.getSheetId() == null) {
                throw new ValidationException("sheetId is required for SHEET items");
            }
            SheetMusicEntity sheetEntity = sheetRepository
                    .findByIdOptional(createCollectionItem.getSheetId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Sheet", createCollectionItem.getSheetId().toString()));
            SheetCollectionItemEntity sheetItem = collectionItemMapper.fromSheetDto(createCollectionItem);
            sheetItem.setSheet(sheetEntity);
            itemEntity = sheetItem;
        } else {
            if (createCollectionItem.getTextContent() == null
                    || createCollectionItem.getTextContent().isBlank()) {
                throw new ValidationException("textContent is required for TEXT items");
            }
            itemEntity = collectionItemMapper.fromTextDto(createCollectionItem);
        }

        collectionItemRepository.persistAndFlush(itemEntity);
        collection.getItems().add(itemEntity);
    }

    public void updateItem(final String collectionId, final String itemId, final CollectionItem dto) {
        CollectionItemEntity itemEntity = collectionItemRepository
                .findByIdOptional(UUID.fromString(itemId))
                .orElseThrow(() -> new EntityNotFoundException("CollectionItem", itemId));
        if (itemEntity instanceof SheetCollectionItemEntity sheetItem) {
            collectionItemMapper.updateSheetItem(sheetItem, dto);
        } else if (itemEntity instanceof TextCollectionItemEntity textItem) {
            collectionItemMapper.updateTextItem(textItem, dto);
        }
    }

    public PaginatedResponse<SheetCollection> findCollectionsForSheet(
            final String sheetId, final PaginationRequest pagination) {
        UUID sheetUuid = UUID.fromString(sheetId);
        PaginatedEntities<SheetCollectionEntity> result = repository.findBySheetId(sheetUuid, pagination);
        PaginatedResponse<SheetCollection> response = new PaginatedResponse<>();
        response.setData(result.data().stream()
                .map(entity -> {
                    SheetCollection dto = mapper.toDto(entity);
                    dto.setItems(dto.getItems().stream()
                            .filter(item -> sheetUuid.equals(item.getSheetId()))
                            .toList());
                    return dto;
                })
                .toList());
        response.setPage(pagination.getPage());
        response.setSize(response.getData().size());
        response.setTotalCount(result.totalCount());
        return response;
    }

    public void reorderItems(final String collectionId, final List<String> orderedIds) {
        SheetCollectionEntity collection = repository
                .findByIdOptional(UUID.fromString(collectionId))
                .orElseThrow(() -> new EntityNotFoundException("SheetCollection", collectionId));
        List<CollectionItemEntity> currentItems = collection.getItems();
        // Validate all IDs belong to this collection before touching anything
        orderedIds.forEach(id -> currentItems.stream()
                .filter(item -> item.getId().toString().equals(id))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("CollectionItem", id)));
        // Update items_order directly via native SQL to avoid Envers inserting both a DEL
        // and an ADD record for the same (rev, collection_id, item_id) within one transaction,
        // which would violate the audit table PK.
        // Two-pass approach: first shift all rows to a high temporary offset so the final
        // 0..N values don't collide with existing ones while rows are being updated one by one
        // (the join table has a PK on (sheet_collections_id, items_order) that enforces
        // uniqueness per statement, so a single-pass update can hit transient duplicates).
        var em = collectionItemRepository.getEntityManager();
        UUID collUUID = UUID.fromString(collectionId);
        int offset = orderedIds.size() + 1000;
        for (int i = 0; i < orderedIds.size(); i++) {
            em.createNativeQuery("UPDATE sheet_collections_items SET items_order = :ord"
                            + " WHERE sheet_collections_id = :collId AND items_id = :itemId")
                    .setParameter("ord", offset + i)
                    .setParameter("collId", collUUID)
                    .setParameter("itemId", UUID.fromString(orderedIds.get(i)))
                    .executeUpdate();
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            em.createNativeQuery("UPDATE sheet_collections_items SET items_order = :ord"
                            + " WHERE sheet_collections_id = :collId AND items_id = :itemId")
                    .setParameter("ord", i)
                    .setParameter("collId", collUUID)
                    .setParameter("itemId", UUID.fromString(orderedIds.get(i)))
                    .executeUpdate();
        }
    }

    public void removeItem(final String collectionId, final String itemId) {
        SheetCollectionEntity collection = repository
                .findByIdOptional(UUID.fromString(collectionId))
                .orElseThrow(() -> new EntityNotFoundException("SheetCollection", collectionId));
        CollectionItemEntity itemEntity = collectionItemRepository
                .findByIdOptional(UUID.fromString(itemId))
                .orElseThrow(() -> new EntityNotFoundException("CollectionItem", itemId));
        if (itemEntity instanceof TextCollectionItemEntity textItem && textItem.getAttachment() != null) {
            cleanUpAttachment(textItem);
        }
        collection.getItems().remove(itemEntity);
        collectionItemRepository.delete(itemEntity);
    }

    public record TextItemAttachment(String identifier, AttachmentEntity attachment) {}

    public List<TextItemAttachment> getTextItemAttachments(final String collectionId) {
        SheetCollectionEntity collection =
                repository.findByIdOptional(UUID.fromString(collectionId)).orElse(null);
        if (collection == null) {
            return List.of();
        }
        return collection.getItems().stream()
                .filter(TextCollectionItemEntity.class::isInstance)
                .map(TextCollectionItemEntity.class::cast)
                .filter(t -> t.getAttachment() != null && t.getAttachment().getDocument() != null)
                .map(t -> new TextItemAttachment(t.getIdentifier() != null ? t.getIdentifier() : "", t.getAttachment()))
                .toList();
    }

    // ── Text item attachment ───────────────────────────────────────────────────

    public Attachment attachDocument(
            final String collectionId, final String itemId, final String filename, final InputStream inputStream)
            throws IOException, NoSuchAlgorithmException {
        TextCollectionItemEntity textItem = loadTextItem(itemId);
        if (textItem.getAttachment() != null) {
            cleanUpAttachment(textItem);
        }
        // save() creates the Document + Attachment (with refcount) in one step
        DocumentUpload upload = documentsService.save(filename, inputStream, AttachmentType.PROGRAM_NOTE);
        AttachmentEntity attachment =
                attachmentRepository.findById(upload.attachment().getId());
        textItem.setAttachment(attachment);
        return upload.attachment();
    }

    public void removeAttachment(final String collectionId, final String itemId) {
        TextCollectionItemEntity textItem = loadTextItem(itemId);
        if (textItem.getAttachment() == null) {
            return;
        }
        cleanUpAttachment(textItem);
    }

    private TextCollectionItemEntity loadTextItem(final String itemId) {
        CollectionItemEntity entity = collectionItemRepository
                .findByIdOptional(UUID.fromString(itemId))
                .orElseThrow(() -> new EntityNotFoundException("CollectionItem", itemId));
        if (!(entity instanceof TextCollectionItemEntity textItem)) {
            throw new ValidationException("Attachments are only supported for TEXT items");
        }
        return textItem;
    }

    private void cleanUpAttachment(final TextCollectionItemEntity textItem) {
        String attachmentId = textItem.getAttachment().getId().toString();
        textItem.setAttachment(null);
        collectionItemRepository.getEntityManager().flush();
        documentsService.deleteAttachment(attachmentId);
    }
}
