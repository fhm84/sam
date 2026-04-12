package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.CreateEnsembleMembership;
import de.halbmann.sam.api.entity.EnsembleMembership;
import de.halbmann.sam.business.entity.EnsembleMembershipEntity;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = {MusicianMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EnsembleMembershipMapper {

    @Mapping(target = "voiceId", source = "voice.id")
    @Mapping(target = "voiceLabel", source = "voice.label")
    @Mapping(target = "instrumentId", source = "instrument.id")
    @Mapping(target = "instrumentName", source = "instrument.name")
    EnsembleMembership toDto(EnsembleMembershipEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "ensemble", ignore = true)
    @Mapping(target = "musician", ignore = true)
    @Mapping(target = "voice", ignore = true)
    @Mapping(target = "instrument", ignore = true)
    EnsembleMembershipEntity fromCreateDto(CreateEnsembleMembership dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "ensemble", ignore = true)
    @Mapping(target = "musician", ignore = true)
    @Mapping(target = "voice", ignore = true)
    @Mapping(target = "instrument", ignore = true)
    void update(@MappingTarget EnsembleMembershipEntity entity, EnsembleMembership dto);
}
