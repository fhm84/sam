package de.halbmann.sam.api.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request data for adding an instrument option to an ensemble voice.
 */
@Data
@EqualsAndHashCode
public class CreateVoiceOption {

    /**
     * Canonical instrument ID (e.g. "TRUMPET_BB").
     */
    @NotBlank
    String instrumentId;

    /**
     * Priority level of this option.
     */
    VoiceOptionType type;

    /**
     * Scoring factor applied when this option is used (0.0 - 1.0).
     */
    double factor;
}
