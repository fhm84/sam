package de.halbmann.sam.business.musicians.controller;

import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.musicians.MusicianContact;
import de.halbmann.sam.api.entity.musicians.MusicianMembership;
import de.halbmann.sam.business.musicians.entity.MusicianEntity;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = {MusicianInstrumentMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MusicianMapper {

    @Mapping(target = "contact.email", source = "email")
    @Mapping(target = "contact.mobile", source = "mobile")
    @Mapping(target = "contact.notes", source = "notes")
    @Mapping(target = "membership.status", source = "status")
    @Mapping(target = "membership.role", source = "role")
    @Mapping(target = "membership.lastInviteSentAt", source = "lastInviteSentAt")
    Musician toDto(final MusicianEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "instruments", ignore = true)
    @Mapping(target = "lastInviteSentAt", ignore = true)
    @Mapping(target = "email", source = "contact.email")
    @Mapping(target = "mobile", source = "contact.mobile")
    @Mapping(target = "notes", source = "contact.notes")
    @Mapping(target = "status", source = "membership.status")
    @Mapping(target = "role", source = "membership.role")
    MusicianEntity fromDto(final Musician dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "instruments", ignore = true)
    @Mapping(target = "lastInviteSentAt", ignore = true)
    @Mapping(target = "email", source = "contact.email")
    @Mapping(target = "mobile", source = "contact.mobile")
    @Mapping(target = "notes", source = "contact.notes")
    @Mapping(target = "status", source = "membership.status")
    @Mapping(target = "role", source = "membership.role")
    void update(@MappingTarget final MusicianEntity entity, final Musician dto);

    @AfterMapping
    default void nullifyEmptySubObjects(@MappingTarget final Musician musician) {
        MusicianContact c = musician.getContact();
        if (c != null && c.getEmail() == null && c.getMobile() == null && c.getNotes() == null) {
            musician.setContact(null);
        }
        MusicianMembership m = musician.getMembership();
        if (m != null && m.getStatus() == null && m.getRole() == null && m.getLastInviteSentAt() == null) {
            musician.setMembership(null);
        }
    }
}
