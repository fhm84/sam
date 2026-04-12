package de.halbmann.sam.api.entity;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request data for adding a musician to an ensemble. Voice and instrument are optional;
 * omitting them creates a generic membership (e.g. a conductor entry).
 */
@Data
@EqualsAndHashCode
public class CreateEnsembleMembership {

    /** ID of the musician to add. */
    @NotNull
    UUID musicianId;

    /** ID of the ensemble voice this musician fills, or null for conductor/unassigned roles. */
    UUID voiceId;

    /** Canonical instrument ID this musician plays in this ensemble (e.g. "TRUMPET_BB"), or null. */
    String instrumentId;

    /** Whether this musician is the conductor of the ensemble. */
    boolean conductor;
}
