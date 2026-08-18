package de.halbmann.sam.business.collections.controller;

import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.business.collections.entity.SheetCollectionEntity;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = {CollectionItemMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SheetCollectionMapper {

    SheetCollection toDto(SheetCollectionEntity entity);

    @AfterMapping
    default void assignOrderNumbers(@MappingTarget SheetCollection dto) {
        var items = dto.getItems();
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setOrderNumber(i);
        }
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "items", ignore = true)
    SheetCollectionEntity fromDto(SheetCollection dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "items", ignore = true)
    void update(@MappingTarget SheetCollectionEntity entity, SheetCollection dto);
}
