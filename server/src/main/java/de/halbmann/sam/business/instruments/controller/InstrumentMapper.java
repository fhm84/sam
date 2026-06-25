package de.halbmann.sam.business.instruments.controller;

import de.halbmann.sam.api.entity.instruments.CreateInstrument;
import de.halbmann.sam.api.entity.instruments.Instrument;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InstrumentMapper {

    Instrument toDto(InstrumentEntity entity);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    InstrumentEntity fromDto(CreateInstrument dto);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    void update(@MappingTarget InstrumentEntity entity, Instrument dto);
}
