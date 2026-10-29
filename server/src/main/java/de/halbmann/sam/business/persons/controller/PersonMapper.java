package de.halbmann.sam.business.persons.controller;

import de.halbmann.sam.api.entity.persons.Person;
import de.halbmann.sam.business.persons.entity.PersonEntity;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.JAKARTA_CDI, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PersonMapper {

    Person toDto(PersonEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    PersonEntity fromDto(Person dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    void update(@MappingTarget PersonEntity entity, Person dto);
}
