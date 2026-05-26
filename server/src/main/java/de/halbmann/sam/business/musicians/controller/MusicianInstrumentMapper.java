package de.halbmann.sam.business.musicians.controller;

import de.halbmann.sam.api.entity.musicians.MusicianInstrument;
import de.halbmann.sam.business.musicians.entity.MusicianInstrumentEntity;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MusicianInstrumentMapper {

    @Mapping(target = "instrumentId", source = "instrument.id")
    @Mapping(target = "instrumentName", source = "instrument.name")
    MusicianInstrument toDto(MusicianInstrumentEntity entity);
}
