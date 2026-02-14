package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.CreateInstrument;
import de.halbmann.sam.api.entity.Instrument;
import de.halbmann.sam.business.entity.InstrumentEntity;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InstrumentMapper {

    Instrument toDto(final InstrumentEntity entity);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    InstrumentEntity fromDto(final CreateInstrument dto);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    void update(@MappingTarget final InstrumentEntity entity, final Instrument dto);
}
