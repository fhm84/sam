package de.halbmann.sam.api.entity.documents;

/**
 * Response returned after a successful document upload. Contains both the raw document metadata
 * and the initially created (unlinked) attachment record.
 */
public record DocumentUpload(DocumentDownload document, Attachment attachment) {}
