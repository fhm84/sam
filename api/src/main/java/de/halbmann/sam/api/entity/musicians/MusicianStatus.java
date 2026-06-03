package de.halbmann.sam.api.entity.musicians;

/** Membership status of a musician within an ensemble. */
public enum MusicianStatus {
    /** Actively participating in the ensemble. */
    ACTIVE,
    /** No longer active; kept for historical references. */
    INACTIVE,
    /** An invitation email has been sent; the musician has not yet accepted. */
    INVITED,
    /** Awaiting admin approval or further action before becoming active. */
    PENDING
}
