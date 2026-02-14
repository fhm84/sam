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

    /**
     * Reference to the instrument by canonical ID (e.g. "TENORHORN_BB").
     */
    @NotBlank
    String instrumentId;

    /**
     * The part number/label (for example: '2' for 2. Bass, or even '3rd' or 'Solo')
     */
    String partLabel;

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
