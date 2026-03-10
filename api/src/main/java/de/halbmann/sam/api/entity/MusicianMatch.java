package de.halbmann.sam.api.entity;

import java.util.UUID;

/**
 * A candidate musician match returned during document classification.
 *
 * @param id    the musician's UUID
 * @param name  the musician's full name
 * @param score trigram similarity score in [0.0, 1.0]; higher is a better match
 */
public record MusicianMatch(UUID id, String name, double score) {}
