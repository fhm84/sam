package de.halbmann.sam.api.entity.members;

/** Whether a member currently belongs to the band. */
public enum MembershipStatus {
    /** Plays in the band. */
    ACTIVE,
    /** Temporarily not playing; data and login are kept. */
    PAUSED,
    /** Has left the band; {@code leftAt} starts the retention period of their personal data. */
    FORMER
}
