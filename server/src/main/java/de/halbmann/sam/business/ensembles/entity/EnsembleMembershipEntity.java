package de.halbmann.sam.business.ensembles.entity;

import de.halbmann.sam.api.entity.ensembles.EnsembleRole;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.business.members.entity.MemberEntity;
import de.halbmann.sam.business.shared.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Getter
@Setter
@Entity
@Audited
@Table(name = "ensemble_memberships")
public class EnsembleMembershipEntity extends AbstractEntity {

    @ManyToOne(optional = false)
    MemberEntity member;

    @ManyToOne(optional = false)
    EnsembleEntity ensemble;

    /** The voice this member fills, or null for conductor/non-playing roles. */
    @ManyToOne
    EnsembleVoiceEntity voice;

    /** The instrument this member plays in this ensemble, or null for conductor roles. */
    @ManyToOne
    InstrumentEntity instrument;

    @Enumerated(EnumType.STRING)
    EnsembleRole role = EnsembleRole.MEMBER;
}
