package de.halbmann.sam.business.entity;

import de.halbmann.sam.api.entity.Clef;
import de.halbmann.sam.api.entity.NotationType;
import jakarta.persistence.*;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Defines individual instrument parts for a piece of sheet music.
 */
@Getter
@Setter
@Entity
@Audited
@Cacheable
@Table(
        name = "instrumentations",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uc_instrumentation",
                        columnNames = {"sheet_id", "instrument_id", "partLabel"}))
public class InstrumentationEntity extends AbstractEntity {

    /**
     * The linked instrument.
     */
    @ManyToOne(optional = false)
    InstrumentEntity instrument;

    /**
     * The part number/label (for example: '2' for 2. Bass, or even '3rd' or 'Solo')
     */
    String partLabel;

    /**
     * Clef type (e.g., Treble, Bass, Alto, Tenor)
     */
    @Enumerated(EnumType.STRING)
    Clef clef;

    /**
     * Type of notation (Standard, Tablature, Percussion)
     */
    @Enumerated(EnumType.STRING)
    NotationType notationType;

    /**
     * Metadata of the sheet music/instrumentation files (including location, mime-type, fileSize,
     * ...)
     */
    @OneToMany(fetch = FetchType.LAZY)
    Set<AttachmentEntity> attachments;

    /**
     * (Optional) notes for the part
     */
    @Column(columnDefinition = "text")
    String notes;

    /**
     * Sheet music entity, the instrumentation belongs to.
     */
    @ManyToOne
    SheetMusicEntity sheet;
}
