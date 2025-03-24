package de.halbmann.sam.api.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * Defines individual instrument parts for a piece of sheet music.
 */
@Data
@EqualsAndHashCode
public class Instrumentation {

    /**
     * Unique identifier of the instrumentation.
     */
    UUID id;
    /**
     * Instrument name (e.g. Trumpet, Violin, Bass)
     */
    @NotBlank
    String instrumentName;
    /**
     * The key (for example: 2 for 2. Bass)
     */
    Integer key;
    /**
     * Specific key signature for this instrument (e.g., Bb Major, C Major)
     */
    InstrumentTransposing keySignature;
    /**
     * Clef type (e.g., Treble, Bass, Alto, Tenor)
     */
    Clef clef;

    /**
     * Type of notation (Standard, Tablature, Percussion)
     */
    NotationType notationType;

    // FIXME: how to store pdf/MIDI-File?
    /**
     * Location of the sheet music file
     */
    String pdfFile;
    /**
     * (Optional) MIDI file location
     */
    String midiFile;

}
