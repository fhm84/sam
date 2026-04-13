package de.halbmann.sam.business.collections.controller;

import de.halbmann.sam.api.entity.collections.CollectionSheet;
import de.halbmann.sam.api.entity.collections.CreateCollectionSheet;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.collections.SheetCollectionFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.business.collections.boundary.CollectionSheetRepository;
import de.halbmann.sam.business.collections.boundary.SheetCollectionRepository;
import de.halbmann.sam.business.collections.entity.CollectionSheetEntity;
import de.halbmann.sam.business.collections.entity.SheetCollectionEntity;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.core.entity.PaginatedEntities;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class SheetCollectionService {

    @Inject
    SheetCollectionRepository repository;

    @Inject
    CollectionSheetRepository collectionSheetRepository;

    @Inject
    SheetCollectionMapper mapper;

    @Inject
    CollectionSheetMapper collectionSheetMapper;

    @Inject
    SheetRepository sheetRepository;

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

    public PaginatedResponse<CollectionSheet> listSheets(
            final String collectionId, final PaginationRequest pagination) {
        SheetCollectionEntity collection = repository
                .findByIdOptional(UUID.fromString(collectionId))
                .orElseThrow(() -> new EntityNotFoundException("SheetCollection", collectionId));
        List<CollectionSheet> sheets = collection.getSheets().stream()
                .map(collectionSheetMapper::toDto)
                .toList();
        PaginatedResponse<CollectionSheet> response = new PaginatedResponse<>();
        response.setData(sheets);
        response.setPage(0);
        response.setSize(sheets.size());
        response.setTotalCount((long) sheets.size());
        return response;
    }

    public void addSheet(final String collectionId, final CreateCollectionSheet createCollectionSheet) {
        SheetCollectionEntity collection = repository
                .findByIdOptional(UUID.fromString(collectionId))
                .orElseThrow(() -> new EntityNotFoundException("SheetCollection", collectionId));
        SheetMusicEntity sheetEntity = sheetRepository
                .findByIdOptional(createCollectionSheet.getSheetId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Sheet", createCollectionSheet.getSheetId().toString()));
        CollectionSheetEntity csEntity = collectionSheetMapper.fromDto(createCollectionSheet);
        csEntity.setSheet(sheetEntity);
        collectionSheetRepository.persistAndFlush(csEntity);
        collection.getSheets().add(csEntity);
    }

    public void updateSheet(final String collectionId, final String sheetId, final CollectionSheet dto) {
        CollectionSheetEntity csEntity = collectionSheetRepository
                .findByIdOptional(UUID.fromString(sheetId))
                .orElseThrow(() -> new EntityNotFoundException("CollectionSheet", sheetId));
        collectionSheetMapper.update(csEntity, dto);
    }

    public PaginatedResponse<SheetCollection> findCollectionsForSheet(
            final String sheetId, final PaginationRequest pagination) {
        UUID sheetUuid = UUID.fromString(sheetId);
        PaginatedEntities<SheetCollectionEntity> result = repository.findBySheetId(sheetUuid, pagination);
        PaginatedResponse<SheetCollection> response = new PaginatedResponse<>();
        response.setData(result.data().stream()
                .map(entity -> {
                    SheetCollection dto = mapper.toDto(entity);
                    // retain only the CollectionSheet entry for this specific sheet
                    dto.setSheets(dto.getSheets().stream()
                            .filter(cs -> sheetUuid.equals(cs.getSheetId()))
                            .toList());
                    return dto;
                })
                .toList());
        response.setPage(pagination.getPage());
        response.setSize(response.getData().size());
        response.setTotalCount(result.totalCount());
        return response;
    }

    public void removeSheet(final String collectionId, final String sheetId) {
        SheetCollectionEntity collection = repository
                .findByIdOptional(UUID.fromString(collectionId))
                .orElseThrow(() -> new EntityNotFoundException("SheetCollection", collectionId));
        CollectionSheetEntity csEntity = collectionSheetRepository
                .findByIdOptional(UUID.fromString(sheetId))
                .orElseThrow(() -> new EntityNotFoundException("CollectionSheet", sheetId));
        collection.getSheets().remove(csEntity);
        collectionSheetRepository.delete(csEntity);
    }
}
