package de.halbmann.sam.api.entity.sheets;

import de.halbmann.sam.api.entity.documents.Attachment;
import de.halbmann.sam.api.entity.instruments.Clef;
import de.halbmann.sam.api.entity.instruments.Instrument;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
     * The linked instrument.
     */
    @NotNull
    Instrument instrument;

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

    /**
     * Physical archive location of the printed copy (e.g. "Cabinet A / Shelf 3 / Folder 12").
     */
    String physicalLocation;

    /**
     * Physical condition of the printed copy in the archive.
     */
    PhysicalCondition physicalCondition;

    /**
     * Metadata of the sheet music files (including location, mime-type, fileSize, ...)
     */
    Set<Attachment> attachments;
}
