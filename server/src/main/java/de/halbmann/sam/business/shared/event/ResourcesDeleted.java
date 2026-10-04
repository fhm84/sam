package de.halbmann.sam.business.shared.event;

import java.util.Set;
import java.util.UUID;

/**
 * Fired synchronously (inside the deleting transaction) when sheets, instrumentations or
 * collections are deleted, so features referencing them by ID without a foreign key — share
 * links — can clean up without the deleting service depending on them.
 */
public record ResourcesDeleted(Set<UUID> resourceIds) {}
