package de.halbmann.sam.api.entity.ensembles;

import de.halbmann.sam.api.entity.members.Member;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a member's seat in an ensemble, optionally tied to a specific voice and instrument. A
 * member may have multiple seats in the same ensemble (one per voice, e.g. when doubling on two
 * instruments), which share one role. A seat without a voice (e.g. the conductor's) has its own role.
 */
@Data
@EqualsAndHashCode
public class EnsembleMembership {

    UUID id;

    /** The member who holds this seat. */
    @NotNull
    Member member;

    /** The voice this member fills, or null for conductor/non-playing roles. */
    UUID voiceId;

    /** Display label of the voice — read-only, populated from the referenced EnsembleVoice. */
    String voiceLabel;

    /** The instrument this member plays in this ensemble, or null for conductor roles. */
    String instrumentId;

    /** Display name of the instrument — read-only, populated from the referenced Instrument. */
    String instrumentName;

    /** The member's role in this ensemble; {@code MEMBER} when omitted. */
    EnsembleRole role;
}
