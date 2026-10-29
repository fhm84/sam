package de.halbmann.sam.business.members.controller;

import de.halbmann.sam.api.entity.members.Member;
import de.halbmann.sam.api.entity.members.MemberContact;
import de.halbmann.sam.api.entity.members.OnboardingState;
import de.halbmann.sam.business.members.entity.MemberEntity;
import de.halbmann.sam.security.PersonalDataAccess;
import jakarta.inject.Inject;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
        uses = {MemberInstrumentMapper.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class MemberMapper {

    @Inject
    PersonalDataAccess personalDataAccess;

    @Mapping(target = "name", source = "person.name")
    @Mapping(target = "contact.email", source = "email")
    @Mapping(target = "contact.mobile", source = "mobile")
    @Mapping(target = "contact.notes", source = "notes")
    @Mapping(target = "status", source = "membershipStatus")
    @Mapping(target = "onboarding", ignore = true)
    public abstract Member toDto(MemberEntity entity);

    @AfterMapping
    void deriveOnboardingAndRedact(final MemberEntity entity, @MappingTarget final Member member) {
        if (entity.getUserId() != null) {
            member.setOnboarding(OnboardingState.LINKED);
        } else if (entity.getLastInviteSentAt() != null) {
            member.setOnboarding(OnboardingState.INVITED);
        } else {
            member.setOnboarding(OnboardingState.NONE);
        }
        // Ensemble seats embed the member: removing personal data here covers every response, not
        // only /members
        if (!personalDataAccess.callerMaySee()) {
            member.setContact(null);
            member.setUserId(null);
            member.setLastInviteSentAt(null);
            member.setOnboarding(null);
            // When someone left starts the retention period of their data: not for everyone
            member.setLeftAt(null);
        }
        MemberContact c = member.getContact();
        if (c != null && c.getEmail() == null && c.getMobile() == null && c.getNotes() == null) {
            member.setContact(null);
        }
    }
}
