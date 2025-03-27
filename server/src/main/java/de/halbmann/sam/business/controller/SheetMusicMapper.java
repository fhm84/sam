package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.SheetMusic;
import de.halbmann.sam.business.boundary.GenreRepository;
import de.halbmann.sam.business.entity.SheetMusicEntity;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = {
                MusicianMapper.class,
                GenreMapper.class,
                InstrumentationMapper.class,
                GenreRepository.class
        },
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SheetMusicMapper {

    @Mapping(target = "genre", source = "genre.name")
    SheetMusic toDto(final SheetMusicEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "instrumentations", ignore = true)
    @Mapping(target = "genre", source = "genre")
    SheetMusicEntity fromDto(final SheetMusic dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "instrumentations", ignore = true)
    @Mapping(target = "genre", source = "genre")
    void update(@MappingTarget final SheetMusicEntity entity, final SheetMusic dto);

}
