package de.halbmann.sam.api.entity;

/**
 * Overall assessment of how well a sheet music's instrumentations cover an ensemble's voice
 * requirements.
 */
public enum CoverageStatus {

    /**
     * All required voices are covered and the overall coverage score is >= 0.85.
     */
    COMPLETE,

    /**
     * No required voices are missing, but the overall coverage score is below 0.85
     * (some voices are under-covered or only partially matched).
     */
    PLAYABLE,

    /**
     * Critical voices are missing or overall coverage is too low for a viable performance.
     */
    INCOMPLETE
}
