package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.CreateEnsemble;
import de.halbmann.sam.api.entity.Ensemble;
import de.halbmann.sam.business.entity.EnsembleEntity;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = {EnsembleVoiceMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EnsembleMapper {

    Ensemble toDto(final EnsembleEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "voices", ignore = true)
    EnsembleEntity fromDto(final CreateEnsemble dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "voices", ignore = true)
    void update(@MappingTarget final EnsembleEntity entity, final Ensemble dto);
}
