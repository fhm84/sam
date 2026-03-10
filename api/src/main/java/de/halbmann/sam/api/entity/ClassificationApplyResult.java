package de.halbmann.sam.api.entity;

import java.util.UUID;

/**
 * IDs of the entities created or resolved when applying a classification.
 */
public record ClassificationApplyResult(UUID sheetId, UUID instrumentationId, UUID attachmentId) {}
