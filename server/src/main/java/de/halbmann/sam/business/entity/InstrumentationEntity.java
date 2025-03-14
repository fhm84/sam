package de.halbmann.sam.business.entity;

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
    String keySignature;
    /**
     * Clef type (e.g., Treble, Bass, Alto, Tenor)
     */
    String clef;

    /**
     * Type of notation (Standard, Tablature, Percussion)
     */
    String notationType;

    // FIXME: how to store pdf/MIDI-File?
    /**
     * Location of the sheet music file
     */
    String pdfFile;
    /**
     * (Optional) MIDI file location
     */
    String midiFile;

    /**
     * Sheet music entity, the instrumentation belongs to.
     */
    @ManyToOne
    SheetMusicEntity sheet;

}
