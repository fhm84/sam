package de.halbmann.sam.business.ensembles.controller;

import de.halbmann.sam.api.entity.ensembles.CreateEnsemble;
import de.halbmann.sam.api.entity.ensembles.Ensemble;
import de.halbmann.sam.business.ensembles.entity.EnsembleEntity;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = {EnsembleVoiceMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EnsembleMapper {

    Ensemble toDto(EnsembleEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "voices", ignore = true)
    EnsembleEntity fromDto(CreateEnsemble dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "voices", ignore = true)
    void update(@MappingTarget EnsembleEntity entity, Ensemble dto);
}
