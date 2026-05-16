package de.halbmann.sam.business.musicians.controller;

import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.business.musicians.entity.MusicianEntity;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MusicianMapper {

    Musician toDto(final MusicianEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    MusicianEntity fromDto(final Musician dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "userId", ignore = true)
    void update(@MappingTarget final MusicianEntity entity, final Musician dto);
}
