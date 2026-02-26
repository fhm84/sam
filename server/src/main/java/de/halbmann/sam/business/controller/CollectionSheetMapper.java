package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.CollectionSheet;
import de.halbmann.sam.api.entity.CreateCollectionSheet;
import de.halbmann.sam.business.entity.CollectionSheetEntity;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CollectionSheetMapper {

    @Mapping(target = "sheetId", source = "sheet.id")
    @Mapping(target = "title", source = "sheet.title")
    @Mapping(target = "subtitle", source = "sheet.subtitle")
    @Mapping(target = "genre", source = "sheet.genre")
    @Mapping(target = "style", source = "sheet.style")
    @Mapping(target = "duration", source = "sheet.duration")
    CollectionSheet toDto(final CollectionSheetEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "sheet", ignore = true)
    CollectionSheetEntity fromDto(final CreateCollectionSheet dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "sheet", ignore = true)
    void update(@MappingTarget final CollectionSheetEntity entity, final CollectionSheet dto);
}
