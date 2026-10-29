package de.halbmann.sam.api.entity.members;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The profile of a person who belongs to the band. It shares its ID with the
 * {@link de.halbmann.sam.api.entity.persons.Person} it belongs to, so a member who arranges is
 * credited on sheets under the same ID. Contact details, the linked login, the invite time and the
 * onboarding state are returned only to the music librarian and the admin.
 */
@Data
@EqualsAndHashCode
public class Member {

    /**
     * The person's ID. On create, an ID of an existing person gives that person a profile; without
     * one, a new person is created from {@link #name}.
     */
    UUID id;

    /** The person's name; changing it renames the person everywhere it is credited. */
    @NotBlank
    String name;

    /** Instruments this member plays (globally, independent of any ensemble membership). */
    List<MemberInstrument> instruments = new ArrayList<>();

    MemberContact contact;

    MembershipStatus status;

    /** When the member left the band; set when the status becomes {@code FORMER}. */
    LocalDate leftAt;

    /** Read-only. */
    LocalDateTime lastInviteSentAt;

    /** Read-only, derived from {@link #userId} and {@link #lastInviteSentAt}. */
    OnboardingState onboarding;

    /**
     * OIDC subject claim linking this member to a login. Read-only here; set with the admin-only
     * link endpoints.
     */
    String userId;
}
