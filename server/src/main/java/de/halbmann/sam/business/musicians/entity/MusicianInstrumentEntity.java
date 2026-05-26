package de.halbmann.sam.business.musicians.entity;

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
        name = "musician_instruments",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "ux_musician_instrument",
                        columnNames = {"musician_id", "instrument_id"}))
public class MusicianInstrumentEntity extends AbstractEntity {

    @ManyToOne(optional = false)
    MusicianEntity musician;

    @ManyToOne(optional = false)
    InstrumentEntity instrument;

    @Column(name = "is_primary")
    boolean primary;
}
