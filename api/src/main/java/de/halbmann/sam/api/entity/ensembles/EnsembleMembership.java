package de.halbmann.sam.api.entity.ensembles;

import de.halbmann.sam.api.entity.musicians.Musician;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a musician's membership in an ensemble, optionally tied to a specific voice and
 * instrument. A musician may have multiple memberships in the same ensemble (one per voice, e.g.
 * when doubling on two instruments). A conductor entry has no voice or instrument.
 */
@Data
@EqualsAndHashCode
public class EnsembleMembership {

    UUID id;

    /** The musician who is a member of this ensemble. */
    @NotNull
    Musician musician;

    /** The voice this musician fills, or null for conductor/non-playing roles. */
    UUID voiceId;

    /** Display label of the voice — read-only, populated from the referenced EnsembleVoice. */
    String voiceLabel;

    /** The instrument this musician plays in this ensemble, or null for conductor roles. */
    String instrumentId;

    /** Display name of the instrument — read-only, populated from the referenced Instrument. */
    String instrumentName;

    /** Whether this musician is the conductor of the ensemble. */
    boolean conductor;
}
