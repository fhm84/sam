package de.halbmann.sam.cli.mapper;

import de.halbmann.sam.api.entity.CreateInstrumentation;
import de.halbmann.sam.api.entity.CreateSheetMusic;
import de.halbmann.sam.api.entity.Instrumentation;
import de.halbmann.sam.api.entity.SheetMusic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DataMapper {

    CreateSheetMusic createFromSheet(final SheetMusic dto);

    @Mapping(target = "instrumentId", source = "instrument.id")
    CreateInstrumentation createFromInstrumentation(final Instrumentation dto);
}
