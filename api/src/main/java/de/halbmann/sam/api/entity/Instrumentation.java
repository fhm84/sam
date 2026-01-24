package de.halbmann.sam.api.entity;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Defines individual instrument parts for a piece of sheet music. */
@Data
@EqualsAndHashCode
public class Instrumentation {

  /** Unique identifier of the instrumentation. */
  UUID id;

  /** Instrument name (e.g. Trumpet, Violin, Bass) */
  @NotBlank String instrumentName;

  /** The part number/label (for example: '2' for 2. Bass, or even '3rd' or 'Solo') */
  String partLabel;

  /** Specific key signature for this instrument (e.g., Bb Major, C Major) */
  InstrumentTransposing transposition;

  /** Clef type (e.g., Treble, Bass, Alto, Tenor) */
  Clef clef;

  /** Type of notation (Standard, Tablature, Percussion) */
  NotationType notationType;

  /** (Optional) notes for the part */
  String notes;

  /** Metadata of the sheet music files (including location, mime-type, fileSize, ...) */
  Set<Attachment> attachments;
}
