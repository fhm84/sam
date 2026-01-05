package de.halbmann.sam.business.entity;

import de.halbmann.sam.api.entity.Clef;
import de.halbmann.sam.api.entity.InstrumentTransposing;
import de.halbmann.sam.api.entity.NotationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.Set;

/**
 * Defines individual instrument parts for a piece of sheet music.
 */
@Getter
@Setter
@Entity
@Audited
@Cacheable
@Table(name = "instrumentations",
        uniqueConstraints = @UniqueConstraint(name = "uc_instrumentation", columnNames = {"sheet_id", "instrumentName", "partLabel", "transposition", "clef"}))
public class InstrumentationEntity extends AbstractEntity {

    /**
     * Instrument name (e.g. Trumpet, Violin, Bass)
     */
    String instrumentName;
    /**
     * The part number/label (for example: '2' for 2. Bass, or even '3rd' or 'Solo')
     */
    String partLabel;
    /**
     * Specific partLabel signature for this instrument (e.g., Bb Major, C Major)
     */
    @Enumerated(EnumType.STRING)
    InstrumentTransposing transposition;
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
     * Metadata of the sheet music/instrumentation files (including location, mime-type, fileSize, ...)
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
