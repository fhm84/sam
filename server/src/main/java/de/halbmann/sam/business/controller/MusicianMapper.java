package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.Musician;
import de.halbmann.sam.business.entity.MusicianEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MusicianMapper {

  Musician toDto(final MusicianEntity entity);

  @Mapping(target = "version", ignore = true)
  @Mapping(target = "created", ignore = true)
  @Mapping(target = "lastUpdate", ignore = true)
  MusicianEntity fromDto(final Musician dto);
}
