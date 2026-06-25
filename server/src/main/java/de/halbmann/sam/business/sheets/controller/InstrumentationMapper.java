package de.halbmann.sam.business.sheets.controller;

import de.halbmann.sam.api.entity.sheets.CreateInstrumentation;
import de.halbmann.sam.api.entity.sheets.Instrumentation;
import de.halbmann.sam.business.documents.controller.AttachmentMapper;
import de.halbmann.sam.business.instruments.controller.InstrumentMapper;
import de.halbmann.sam.business.sheets.entity.InstrumentationEntity;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = {AttachmentMapper.class, InstrumentMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InstrumentationMapper {

    Instrumentation toDto(InstrumentationEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "sheet", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "instrument", ignore = true)
    InstrumentationEntity fromDto(CreateInstrumentation dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "sheet", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "instrument", ignore = true)
    void update(@MappingTarget InstrumentationEntity entity, Instrumentation dto);
}
