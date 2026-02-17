package de.halbmann.sam.api.entity;

/**
 * Overall assessment of how well a sheet music's instrumentations cover an ensemble's voice
 * requirements.
 */
public enum CoverageStatus {

    /**
     * All voices are covered with high-quality matches (score >= 0.9, no missing required voices).
     */
    COMPLETE,

    /**
     * Enough voices are covered to perform the piece, though some gaps exist (score >= 0.5, no
     * missing required voices).
     */
    PLAYABLE,

    /**
     * Critical voices are missing or overall coverage is too low for a viable performance.
     */
    INCOMPLETE
}
