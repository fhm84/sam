package de.halbmann.sam.api.entity.musicians;

/** Role of a musician within an ensemble. */
public enum MusicianRole {
    /** Regular ensemble member. */
    MEMBER,
    /** Occasional participant, not a permanent member. */
    GUEST,
    /** Fills in for absent members; not a permanent seat. */
    SUBSTITUTE,
    /** Leads the ensemble. */
    CONDUCTOR
}
