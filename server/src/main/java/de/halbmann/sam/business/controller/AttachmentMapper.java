package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.Attachment;
import de.halbmann.sam.business.entity.AttachmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AttachmentMapper {

    @Mapping(target = "mimeType", source = "document.mimeType")
    @Mapping(target = "fileSize", source = "document.size")
    @Mapping(target = "checksum", source = "document.sha256")
    Attachment toDto(AttachmentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "uploadedBy", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    @Mapping(target = "document", ignore = true)
    AttachmentEntity fromDto(Attachment dto);

}
