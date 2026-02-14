package de.halbmann.sam.api.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Canonical musical entity for an instrument.
 */
@Data
@EqualsAndHashCode
public class Instrument {

    String id; // "TENORHORN_BB"

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
