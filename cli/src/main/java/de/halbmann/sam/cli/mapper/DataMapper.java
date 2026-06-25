package de.halbmann.sam.cli.mapper;

import de.halbmann.sam.api.entity.sheets.CreateInstrumentation;
import de.halbmann.sam.api.entity.sheets.CreateSheetMusic;
import de.halbmann.sam.api.entity.sheets.Instrumentation;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DataMapper {

    CreateSheetMusic createFromSheet(SheetMusic dto);

    @Mapping(target = "instrumentId", source = "instrument.id")
    CreateInstrumentation createFromInstrumentation(Instrumentation dto);
}
