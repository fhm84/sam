package de.halbmann.sam.api.entity.ensembles;

/** A member's role in one ensemble. */
public enum EnsembleRole {
    /** Regular member. */
    MEMBER,
    /** Occasional participant, not a permanent member. */
    GUEST,
    /** Fills in for absent members; not a permanent seat. */
    SUBSTITUTE,
    /** Leads the ensemble. */
    CONDUCTOR
}
