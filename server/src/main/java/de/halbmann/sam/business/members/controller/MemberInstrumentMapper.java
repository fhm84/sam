package de.halbmann.sam.business.members.controller;

import de.halbmann.sam.api.entity.members.MemberInstrument;
import de.halbmann.sam.business.members.entity.MemberInstrumentEntity;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MemberInstrumentMapper {

    @Mapping(target = "instrumentId", source = "instrument.id")
    @Mapping(target = "instrumentName", source = "instrument.name")
    MemberInstrument toDto(MemberInstrumentEntity entity);
}
