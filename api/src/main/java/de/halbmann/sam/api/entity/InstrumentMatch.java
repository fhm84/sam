package de.halbmann.sam.api.entity;

/**
 * A candidate instrument match returned during document classification.
 *
 * @param id    the instrument's string ID in the database
 * @param name  the instrument's display name
 * @param score trigram similarity score in [0.0, 1.0]; higher is a better match
 */
public record InstrumentMatch(String id, String name, double score) {}
