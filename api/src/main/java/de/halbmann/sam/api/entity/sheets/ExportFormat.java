package de.halbmann.sam.api.entity.sheets;

/**
 * Supported export formats for sheet music and collection exports.
 */
public enum ExportFormat {

    /**
     * ZIP archive containing metadata JSON and all attachment files.
     */
    ZIP,
    /**
     * Structured JSON of the entity data (no attachments).
     */
    JSON,
    /**
     * Flat CSV with sheet metadata fields (no attachments).
     */
    CSV
}
