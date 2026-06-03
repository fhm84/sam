package de.halbmann.sam.api.entity.classification;

/** Lifecycle state of an AI document classification run. */
public enum ClassificationStatus {
    /** Classification has been requested but not yet started. */
    PENDING,
    /** Classification completed and a result is available for review. */
    CLASSIFIED,
    /** Classification result was rejected by the user; the document remains unlinked. */
    REJECTED
}
