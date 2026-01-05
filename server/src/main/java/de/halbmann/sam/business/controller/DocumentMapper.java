package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.DocumentDownload;
import de.halbmann.sam.business.entity.DocumentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DocumentMapper {

    @Mapping(target = "stream", ignore = true)
    @Mapping(target = "checksumSha256", source = "sha256")
    DocumentDownload toDto(DocumentEntity documentEntity);

}
