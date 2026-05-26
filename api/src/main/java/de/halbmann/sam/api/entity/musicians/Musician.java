package de.halbmann.sam.api.entity.musicians;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Stores information about musicians (composers and arrangers).
 */
@Data
@EqualsAndHashCode
public class Musician {

    UUID id;

    @NotBlank
    String name;

    Integer birthYear;

    Integer deathYear;

    /**
     * Interested Party Information (IPI)-Number
     */
    String ipi;

    /**
     * Instruments this musician plays (globally, independent of any ensemble membership).
     */
    List<MusicianInstrument> instruments = new ArrayList<>();

    /**
     * Contact details — null for historical/external musicians (composers, arrangers)
     * who have no active system presence.
     */
    MusicianContact contact;

    /**
     * Ensemble membership context — null for external contributors with no membership role.
     */
    MusicianMembership membership;

    /**
     * OIDC subject claim linking this musician to an authenticated user account.
     * Null for external/historical musicians (composers, arrangers) with no system login.
     */
    String userId;
}
