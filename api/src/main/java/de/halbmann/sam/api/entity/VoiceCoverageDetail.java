package de.halbmann.sam.api.entity;

import lombok.Data;

/**
 * Detailed coverage information for a single ensemble voice. Explains which instrumentation was
 * matched (if any), the quality of the match, and a human-readable explanation.
 */
@Data
public class VoiceCoverageDetail {

    /**
     * Label of the ensemble voice being evaluated (e.g. "1. Trumpet").
     */
    String voiceLabel;

    /**
     * Whether this voice is required for the ensemble to be playable.
     */
    boolean required;

    /**
     * The weight of this voice in the overall coverage score.
     */
    double weight;

    /**
     * Canonical ID of the instrument that best matched this voice, or {@code null} if no match was
     * found.
     */
    String matchedInstrumentId;

    /**
     * Quality of the instrument match (0.0 - 1.0), considering transposition, clef, and notation
     * type compatibility.
     */
    double matchScore;

    /**
     * The voice option's scoring factor that was applied (reflects primary/alternate/fallback
     * preference).
     */
    double optionFactor;

    /**
     * Human-readable explanation of how the match was determined.
     */
    String explanation;
}
