package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.Instrumentation;
import de.halbmann.sam.business.entity.InstrumentationEntity;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InstrumentationMapper {

    Instrumentation toDto(final InstrumentationEntity entity);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "sheet", ignore = true)
    InstrumentationEntity fromDto(final Instrumentation dto);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "sheet", ignore = true)
    void update(@MappingTarget final InstrumentationEntity entity, final Instrumentation dto);

}
