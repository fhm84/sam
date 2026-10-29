package de.halbmann.sam.api.entity.persons;

import java.util.UUID;

/**
 * A candidate person match returned during document classification.
 *
 * @param id    the person's UUID
 * @param name  the person's full name
 * @param score trigram similarity score in [0.0, 1.0]; higher is a better match
 */
public record PersonMatch(UUID id, String name, double score) {}
