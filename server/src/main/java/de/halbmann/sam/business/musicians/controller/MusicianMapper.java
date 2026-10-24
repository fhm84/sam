package de.halbmann.sam.business.musicians.controller;

import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.musicians.MusicianContact;
import de.halbmann.sam.api.entity.musicians.MusicianMembership;
import de.halbmann.sam.business.musicians.entity.MusicianEntity;
import de.halbmann.sam.security.PersonalDataAccess;
import jakarta.inject.Inject;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = {MusicianInstrumentMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class MusicianMapper {

    @Inject
    PersonalDataAccess personalDataAccess;

    @Mapping(target = "contact.email", source = "email")
    @Mapping(target = "contact.mobile", source = "mobile")
    @Mapping(target = "contact.notes", source = "notes")
    @Mapping(target = "membership.status", source = "status")
    @Mapping(target = "membership.role", source = "role")
    @Mapping(target = "membership.lastInviteSentAt", source = "lastInviteSentAt")
    public abstract Musician toDto(MusicianEntity entity);

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
    public abstract MusicianEntity fromDto(Musician dto);

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
    public abstract void update(@MappingTarget MusicianEntity entity, Musician dto);

    @AfterMapping
    void redactAndNullifyEmptySubObjects(@MappingTarget final Musician musician) {
        // Sheets embed composer and arranger, ensemble members embed the musician: removing
        // personal data here covers every response, not only /musicians
        if (!personalDataAccess.callerMaySee()) {
            musician.setContact(null);
            musician.setUserId(null);
            if (musician.getMembership() != null) {
                musician.getMembership().setLastInviteSentAt(null);
            }
        }
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
