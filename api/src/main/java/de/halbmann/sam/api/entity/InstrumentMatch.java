package de.halbmann.sam.api.entity;

/**
 * A candidate instrument match returned during document classification.
 *
 * @param id            the instrument's string ID in the database
 * @param name          the instrument's canonical name
 * @param displayName   optional shorter display name (may be null)
 * @param transposition the concert-pitch key this instrument sounds in (may be null)
 * @param score         trigram similarity score in [0.0, 1.0]; higher is a better match
 */
public record InstrumentMatch(String id, String name, String displayName, String transposition, double score) {}
