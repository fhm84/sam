package de.halbmann.sam.business.shares.controller;

import de.halbmann.sam.api.entity.shares.ShareResponse;
import de.halbmann.sam.business.shares.entity.ShareEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ShareMapper {

    @Mapping(target = "revoked", expression = "java(entity.isRevoked())")
    @Mapping(target = "resourceLabel", ignore = true)
    ShareResponse toDto(ShareEntity entity);
}
