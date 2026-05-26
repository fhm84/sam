package de.halbmann.sam.api.entity.musicians;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MusicianMembership {

    MusicianStatus status;

    MusicianRole role;

    LocalDateTime lastInviteSentAt;
}
