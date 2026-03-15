package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.CreateSheetMusic;
import de.halbmann.sam.api.entity.SheetMusic;
import de.halbmann.sam.business.entity.SheetMusicEntity;
import java.util.Set;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = {MusicianMapper.class, InstrumentationMapper.class, AttachmentMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SheetMusicMapper {

    SheetMusic toDto(final SheetMusicEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "tags", source = "tags", qualifiedByName = "toLowerSet")
    @Mapping(target = "composer", ignore = true)
    @Mapping(target = "arranger", ignore = true)
    @Mapping(target = "instrumentations", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "fingerprint", ignore = true)
    SheetMusicEntity fromDto(final CreateSheetMusic dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "tags", source = "tags", qualifiedByName = "toLowerSet")
    @Mapping(target = "composer", ignore = true)
    @Mapping(target = "arranger", ignore = true)
    @Mapping(target = "instrumentations", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "fingerprint", ignore = true)
    void update(@MappingTarget final SheetMusicEntity entity, final SheetMusic dto);

    // Dedicated method for the Set field
    @Named("toLowerSet")
    @IterableMapping(qualifiedByName = "toLowerCase")
    Set<String> mapSetToLowerCase(Set<String> source);

    // Individual string transformation
    @Named("toLowerCase")
    default String toLowerCase(String source) {
        return source != null ? source.toLowerCase() : null;
    }
}
