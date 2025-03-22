package de.halbmann.sam.business.entity;

import de.halbmann.sam.api.entity.Clef;
import de.halbmann.sam.api.entity.InstrumentTransposing;
import de.halbmann.sam.api.entity.NotationType;
import jakarta.persistence.*;
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
@Table(name = "instrumentations",
        uniqueConstraints = @UniqueConstraint(name = "uc_instrumentation", columnNames = {"sheet_id", "instrumentName", "key", "keySignature", "clef"}))
public class InstrumentationEntity extends AbstractEntity {

    /**
     * Instrument name (e.g. Trumpet, Violin, Bass)
     */
    String instrumentName;
    /**
     * The key (for example: 2 for 2. Bass)
     */
    Integer key;
    /**
     * Specific key signature for this instrument (e.g., Bb Major, C Major)
     */
    @Enumerated(EnumType.STRING)
    InstrumentTransposing keySignature;
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
     * Metadata of the sheet music file (including location, mime-type, fileSize, ...)
     */
    @OneToOne(fetch = FetchType.LAZY)
    Document pdfFile;
    /**
     * (Optional) MIDI file location
     */
    @OneToOne(fetch = FetchType.LAZY)
    Document midiFile;

    /**
     * Sheet music entity, the instrumentation belongs to.
     */
    @ManyToOne
    SheetMusicEntity sheet;

}
