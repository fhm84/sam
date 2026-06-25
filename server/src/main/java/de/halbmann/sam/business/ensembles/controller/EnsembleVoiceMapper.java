package de.halbmann.sam.business.ensembles.controller;

import de.halbmann.sam.api.entity.ensembles.CreateEnsembleVoice;
import de.halbmann.sam.api.entity.ensembles.EnsembleVoice;
import de.halbmann.sam.business.ensembles.entity.EnsembleVoiceEntity;
import de.halbmann.sam.business.sheets.controller.VoiceOptionMapper;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = {VoiceOptionMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EnsembleVoiceMapper {

    EnsembleVoice toDto(EnsembleVoiceEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "ensemble", ignore = true)
    @Mapping(target = "options", ignore = true)
    EnsembleVoiceEntity fromDto(CreateEnsembleVoice dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "ensemble", ignore = true)
    @Mapping(target = "options", ignore = true)
    void update(@MappingTarget EnsembleVoiceEntity entity, EnsembleVoice dto);
}
