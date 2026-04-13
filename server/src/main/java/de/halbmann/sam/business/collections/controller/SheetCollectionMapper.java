package de.halbmann.sam.business.collections.controller;

import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.business.collections.entity.SheetCollectionEntity;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = {CollectionSheetMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SheetCollectionMapper {

    SheetCollection toDto(final SheetCollectionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "sheets", ignore = true)
    SheetCollectionEntity fromDto(final SheetCollection dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "sheets", ignore = true)
    void update(@MappingTarget final SheetCollectionEntity entity, final SheetCollection dto);
}
