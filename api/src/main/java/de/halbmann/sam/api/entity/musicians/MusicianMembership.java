package de.halbmann.sam.api.entity.musicians;

import java.time.LocalDateTime;
import lombok.Data;

/** Ensemble membership details embedded in a musician DTO. */
@Data
public class MusicianMembership {

    MusicianStatus status;

    MusicianRole role;

    LocalDateTime lastInviteSentAt;
}
