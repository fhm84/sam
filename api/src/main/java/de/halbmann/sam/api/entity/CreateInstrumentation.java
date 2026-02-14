package de.halbmann.sam.api.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Defines request data for creating an individual instrument part for a piece of sheet music.
 */
@Data
@EqualsAndHashCode
public class CreateInstrumentation {

    // TODO: remove instrumentName and transposition -> link Instrument (id) instead!

    /**
     * Instrument name (e.g. Trumpet, Violin, Bass)
     */
    @NotBlank
    String instrumentName;

    /**
     * The part number/label (for example: '2' for 2. Bass, or even '3rd' or 'Solo')
     */
    String partLabel;

    /**
     * Specific key signature for this instrument (e.g., Bb Major, C Major)
     */
    InstrumentTransposing transposition;

    /**
     * Clef type (e.g., Treble, Bass, Alto, Tenor)
     */
    Clef clef;

    /**
     * Type of notation (Standard, Tablature, Percussion)
     */
    NotationType notationType;

    /**
     * (Optional) notes for the part
     */
    String notes;

}
