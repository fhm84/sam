package de.halbmann.sam.business.collections.controller;

import de.halbmann.sam.api.entity.collections.CollectionItem;
import de.halbmann.sam.api.entity.collections.CreateCollectionItem;
import de.halbmann.sam.business.collections.entity.CollectionItemEntity;
import de.halbmann.sam.business.collections.entity.SheetCollectionItemEntity;
import de.halbmann.sam.business.collections.entity.TextCollectionItemEntity;
import de.halbmann.sam.business.documents.controller.AttachmentMapper;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        uses = {AttachmentMapper.class})
public interface CollectionItemMapper {

    default CollectionItem toDto(final CollectionItemEntity entity) {
        if (entity instanceof SheetCollectionItemEntity sheet) {
            return toSheetItemDto(sheet);
        } else if (entity instanceof TextCollectionItemEntity text) {
            return toTextItemDto(text);
        }
        throw new IllegalArgumentException("Unknown CollectionItemEntity subtype: " + entity.getClass());
    }

    @Mapping(target = "type", constant = "SHEET")
    @Mapping(target = "sheetId", source = "sheet.id")
    @Mapping(target = "title", source = "sheet.title")
    @Mapping(target = "subtitle", source = "sheet.subtitle")
    @Mapping(target = "genre", source = "sheet.genre")
    @Mapping(target = "style", source = "sheet.style")
    @Mapping(target = "duration", source = "sheet.duration")
    @Mapping(target = "textContent", ignore = true)
    @Mapping(target = "attachment", ignore = true)
    CollectionItem toSheetItemDto(SheetCollectionItemEntity entity);

    @Mapping(target = "type", constant = "TEXT")
    @Mapping(target = "sheetId", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "subtitle", ignore = true)
    @Mapping(target = "genre", ignore = true)
    @Mapping(target = "style", ignore = true)
    @Mapping(target = "duration", ignore = true)
    CollectionItem toTextItemDto(TextCollectionItemEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "sheet", ignore = true)
    SheetCollectionItemEntity fromSheetDto(CreateCollectionItem dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "attachment", ignore = true)
    TextCollectionItemEntity fromTextDto(CreateCollectionItem dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "sheet", ignore = true)
    void updateSheetItem(@MappingTarget SheetCollectionItemEntity entity, CollectionItem dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "attachment", ignore = true)
    void updateTextItem(@MappingTarget TextCollectionItemEntity entity, CollectionItem dto);
}
