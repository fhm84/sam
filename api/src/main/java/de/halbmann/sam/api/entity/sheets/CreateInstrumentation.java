package de.halbmann.sam.api.entity.sheets;

import de.halbmann.sam.api.entity.instruments.Clef;
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
     * Number of pages of the printed part (free text, e.g. "1–2" or "6").
     */
    String pages;

    /**
     * (Optional) notes for the part
     */
    String notes;

    /**
     * Physical archive location of the printed copy (e.g. "Cabinet A / Shelf 3 / Folder 12").
     */
    String physicalLocation;

    /**
     * Physical condition of the printed copy in the archive.
     */
    PhysicalCondition physicalCondition;
}
