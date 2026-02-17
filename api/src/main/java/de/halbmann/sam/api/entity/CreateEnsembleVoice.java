package de.halbmann.sam.api.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request data for creating a new voice within an ensemble. Options (instrument assignments) are
 * added separately via the options sub-resource.
 */
@Data
@EqualsAndHashCode
public class CreateEnsembleVoice {

    /**
     * Human-readable label for this voice (e.g. "1. Trumpet", "Tuba").
     */
    @NotBlank
    String label;

    /**
     * Relative importance of this voice in coverage scoring.
     */
    double weight;

    /**
     * Whether this voice must be covered for the ensemble to be considered playable.
     */
    boolean required;
}
