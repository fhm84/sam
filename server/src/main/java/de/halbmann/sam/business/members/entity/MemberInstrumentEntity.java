package de.halbmann.sam.business.members.entity;

import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.business.shared.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Getter
@Setter
@Entity
@Audited
@Table(
        name = "member_instruments",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "ux_member_instrument",
                        columnNames = {"member_id", "instrument_id"}))
public class MemberInstrumentEntity extends AbstractEntity {

    @ManyToOne(optional = false)
    MemberEntity member;

    @ManyToOne(optional = false)
    InstrumentEntity instrument;

    @Column(name = "is_primary")
    boolean primary;
}
