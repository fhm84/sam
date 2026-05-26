package de.halbmann.sam.api.entity.instruments;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
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

    InstrumentFamily family;

    Clef defaultClef;

    List<String> aliases = new ArrayList<>();

    String catalogSection; // e.g. "Brass · High"

    Integer catalogPosition; // ordering within the section

    /**
     * Specific key signature for this instrument (e.g., Bb Major, C Major)
     */
    InstrumentTransposing transposition;
}
