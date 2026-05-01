package de.halbmann.sam.business.eventlog.controller;

import de.halbmann.sam.api.entity.eventlog.EventLogEntry;
import de.halbmann.sam.business.eventlog.entity.EventLogEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EventLogMapper {

    EventLogEntry toDto(EventLogEntity entity);
}
