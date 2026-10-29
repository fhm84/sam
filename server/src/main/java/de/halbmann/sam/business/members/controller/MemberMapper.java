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

    /**
     * The member as other people see them: personal data removed unless the caller may see it.
     * Ensemble seats embed the member through this, so it covers every response, not only
     * {@code /members}.
     */
    public Member toDto(final MemberEntity entity) {
        final Member member = toUnredactedDto(entity);
        if (member != null && !personalDataAccess.callerMaySee()) {
            member.setContact(null);
            member.setUserId(null);
            member.setLastInviteSentAt(null);
            member.setOnboarding(null);
            // When someone left starts the retention period of their data: not for everyone
            member.setLeftAt(null);
        }
        return member;
    }

    /** All of the member's data, for the member themselves (their own data) and internal use. */
    @Named("unredacted")
    @Mapping(target = "name", source = "person.name")
    @Mapping(target = "contact.email", source = "email")
    @Mapping(target = "contact.mobile", source = "mobile")
    @Mapping(target = "contact.notes", source = "notes")
    @Mapping(target = "status", source = "membershipStatus")
    @Mapping(target = "onboarding", ignore = true)
    public abstract Member toUnredactedDto(MemberEntity entity);

    @AfterMapping
    void deriveOnboarding(final MemberEntity entity, @MappingTarget final Member member) {
        if (entity.getUserId() != null) {
            member.setOnboarding(OnboardingState.LINKED);
        } else if (entity.getLastInviteSentAt() != null) {
            member.setOnboarding(OnboardingState.INVITED);
        } else {
            member.setOnboarding(OnboardingState.NONE);
        }
        MemberContact c = member.getContact();
        if (c != null && c.getEmail() == null && c.getMobile() == null && c.getNotes() == null) {
            member.setContact(null);
        }
    }
}
