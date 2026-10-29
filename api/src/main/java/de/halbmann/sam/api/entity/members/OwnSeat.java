package de.halbmann.sam.api.entity.members;

import de.halbmann.sam.api.entity.ensembles.EnsembleRole;
import java.util.UUID;
import lombok.Data;

/** One of the caller's own ensemble seats, with the names needed to show it. */
@Data
public class OwnSeat {

    UUID ensembleId;

    String ensembleName;

    /** Null for a seat without a voice (e.g. the conductor's). */
    String voiceLabel;

    String instrumentName;

    EnsembleRole role;
}
