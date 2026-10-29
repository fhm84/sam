package de.halbmann.sam.business.ensembles.controller;

import de.halbmann.sam.api.entity.ensembles.CreateEnsembleMembership;
import de.halbmann.sam.api.entity.ensembles.EnsembleMembership;
import de.halbmann.sam.business.ensembles.entity.EnsembleMembershipEntity;
import de.halbmann.sam.business.members.controller.MemberMapper;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = {MemberMapper.class},
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
    @Mapping(target = "member", ignore = true)
    @Mapping(target = "voice", ignore = true)
    @Mapping(target = "instrument", ignore = true)
    EnsembleMembershipEntity fromCreateDto(CreateEnsembleMembership dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "ensemble", ignore = true)
    @Mapping(target = "member", ignore = true)
    @Mapping(target = "voice", ignore = true)
    @Mapping(target = "instrument", ignore = true)
    void update(@MappingTarget EnsembleMembershipEntity entity, EnsembleMembership dto);
}
