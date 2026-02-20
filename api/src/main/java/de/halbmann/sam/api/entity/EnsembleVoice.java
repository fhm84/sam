package de.halbmann.sam.api.entity;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A single voice (instrument slot) within an ensemble. Each voice defines a role that needs to be
 * filled (e.g. "1. Trumpet", "Tuba", "Percussion") and carries a weight indicating its importance
 * for coverage scoring.
 */
@Data
@EqualsAndHashCode
public class EnsembleVoice {

    /**
     * Unique identifier of the voice.
     */
    UUID id;

    /**
     * Human-readable label for this voice (e.g. "1. Trumpet", "Tuba", "Drums").
     */
    @NotBlank
    String label;

    /**
     * Relative importance of this voice in the overall coverage score. Higher weight means this
     * voice contributes more to the final score.
     */
    double weight;

    /**
     * Whether this voice is mandatory. If a required voice has no matching instrumentation, the
     * overall status will be {@link CoverageStatus#INCOMPLETE} regardless of total score.
     */
    boolean required;

    int minCount;
    int targetCount;
    int maxCount;

    /**
     * Instrument options that can fill this voice, ordered by preference (primary, alternate,
     * fallback).
     */
    List<VoiceOption> options;
}
