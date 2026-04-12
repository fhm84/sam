package de.halbmann.sam.business.entity;

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
    MusicianEntity musician;

    @ManyToOne(optional = false)
    EnsembleEntity ensemble;

    /** The voice this musician fills, or null for conductor/non-playing roles. */
    @ManyToOne
    EnsembleVoiceEntity voice;

    /** The instrument this musician plays in this ensemble, or null for conductor roles. */
    @ManyToOne
    InstrumentEntity instrument;

    boolean conductor;
}
