package de.halbmann.sam.business.members.entity;

import de.halbmann.sam.api.entity.members.MembershipStatus;
import de.halbmann.sam.business.persons.entity.PersonEntity;
import de.halbmann.sam.business.shared.entity.AbstractBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * The profile of a person who belongs to the band: the personal data (contact details, login link,
 * invite time) and the instruments they play. Its ID is the person's ID (ADR-0015).
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "members")
public class MemberEntity extends AbstractBaseEntity {

    @Id
    @Setter(AccessLevel.NONE)
    UUID id;

    /** Read-only view of the shared primary key; set with {@link #setPerson(PersonEntity)}. */
    @OneToOne(optional = false)
    @PrimaryKeyJoinColumn
    @NotAudited
    @Setter(AccessLevel.NONE)
    PersonEntity person;

    String email;

    String mobile;

    @Column(columnDefinition = "text")
    String notes;

    @Enumerated(EnumType.STRING)
    MembershipStatus membershipStatus = MembershipStatus.ACTIVE;

    LocalDate leftAt;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    List<MemberInstrumentEntity> instruments = new ArrayList<>();

    LocalDateTime lastInviteSentAt;

    @Column(unique = true)
    String userId;

    /** Makes this the profile of {@code person}, which must already have its ID. */
    public void setPerson(final PersonEntity person) {
        this.person = person;
        this.id = person.getId();
    }
}
