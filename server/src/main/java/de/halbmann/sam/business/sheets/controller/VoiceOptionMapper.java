package de.halbmann.sam.business.sheets.controller;

import de.halbmann.sam.api.entity.instruments.CreateVoiceOption;
import de.halbmann.sam.api.entity.instruments.VoiceOption;
import de.halbmann.sam.business.sheets.entity.VoiceOptionEntity;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface VoiceOptionMapper {

    @Mapping(target = "instrumentId", source = "instrument.id")
    VoiceOption toDto(VoiceOptionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "voice", ignore = true)
    @Mapping(target = "instrument", ignore = true)
    VoiceOptionEntity fromDto(CreateVoiceOption dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "voice", ignore = true)
    @Mapping(target = "instrument", ignore = true)
    void update(@MappingTarget VoiceOptionEntity entity, VoiceOption dto);
}
