package de.halbmann.sam.api.entity.eventlog;

/**
 * Business event types recorded in the event log. Covers read-side access events that are not
 * captured by the Hibernate Envers audit trail (which only tracks data mutations).
 */
public enum EventType {

    /** A single document was served to a user (inline preview or explicit download). */
    DOCUMENT_DOWNLOAD,

    /** A batch of documents was packaged and downloaded as a ZIP archive or merged PDF. */
    DOCUMENT_BATCH_DOWNLOAD,

    /** A single sheet was exported (JSON, CSV, or ZIP of attached documents). */
    SHEET_EXPORT,

    /** All sheets in a collection were exported as a ZIP or CSV. */
    COLLECTION_EXPORT,

    /** A table-of-contents PDF was generated for a collection. */
    COLLECTION_TOC_GENERATED,

    /** A GEMA setlist workbook was generated for a collection. */
    GEMA_SETLIST_GENERATED,

    /** AI classification was run on a document, producing a {@code SheetClassification} result. */
    DOCUMENT_CLASSIFIED,

    /** A reviewed AI classification was applied, creating or updating sheet/musician/instrument entities. */
    DOCUMENT_CLASSIFICATION_APPLIED,
}
