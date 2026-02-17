package de.halbmann.sam.api.entity;

/**
 * Defines the priority level of an instrument option within an ensemble voice. Determines how
 * desirable a particular instrument assignment is when evaluating coverage.
 */
public enum VoiceOptionType {

    /**
     * The ideal instrument for this voice (e.g. Trumpet in Bb for a "1. Trumpet" voice).
     */
    PRIMARY,

    /**
     * An acceptable substitute that can fill the voice adequately (e.g. Flugelhorn instead of
     * Trumpet).
     */
    ALTERNATE,

    /**
     * A last-resort option that technically works but is not ideal (e.g. Clarinet covering a
     * Trumpet part).
     */
    FALLBACK
}
