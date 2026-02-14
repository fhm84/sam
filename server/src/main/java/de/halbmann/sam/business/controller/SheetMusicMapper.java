package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.CreateSheetMusic;
import de.halbmann.sam.api.entity.SheetMusic;
import de.halbmann.sam.business.boundary.GenreRepository;
import de.halbmann.sam.business.entity.SheetMusicEntity;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = {
            MusicianMapper.class,
            GenreMapper.class,
            InstrumentationMapper.class,
            AttachmentMapper.class,
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
    @Mapping(target = "genre", source = "genre")
    @Mapping(target = "instrumentations", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "fingerprint", ignore = true)
    SheetMusicEntity fromDto(final CreateSheetMusic dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "genre", source = "genre")
    @Mapping(target = "instrumentations", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "fingerprint", ignore = true)
    void update(@MappingTarget final SheetMusicEntity entity, final SheetMusic dto);
}
