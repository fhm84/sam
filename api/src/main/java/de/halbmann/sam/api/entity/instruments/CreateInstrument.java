package de.halbmann.sam.api.entity.instruments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Defines request data for creating a canonical musical entity for an instrument.
 */
@Data
@EqualsAndHashCode
public class CreateInstrument {

    /**
     * Canonical ID like "TENORHORN_BB"
     */
    @NotBlank
    @Pattern(regexp = "^[-_A-Z0-9]+(_[A-Z0-9]+)*$")
    String id;

    /**
     * Instrument name (e.g. Trumpet, Violin, Bass)
     */
    @NotBlank
    String name;

    String displayName; // "Tenorhorn in B"
    // InstrumentFamily family;    // BRASS
    // InstrumentRole role;        // MIDDLE
    /**
     * Specific key signature for this instrument (e.g., Bb Major, C Major)
     */
    InstrumentTransposing transposition;
}
