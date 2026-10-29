package de.halbmann.sam.api.entity.ensembles;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request data for adding a member to an ensemble. Voice and instrument are optional; omitting them
 * creates a seat without a voice (e.g. a conductor entry).
 */
@Data
@EqualsAndHashCode
public class CreateEnsembleMembership {

    /** ID of the member to add. */
    @NotNull
    UUID memberId;

    /** ID of the ensemble voice this member fills, or null for conductor/unassigned roles. */
    UUID voiceId;

    /** Canonical instrument ID this member plays in this ensemble (e.g. "TRUMPET_BB"), or null. */
    String instrumentId;

    /** The member's role in this ensemble; {@code MEMBER} when omitted. */
    EnsembleRole role;
}
