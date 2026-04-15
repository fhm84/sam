package de.halbmann.sam.business.documents.controller;

import de.halbmann.sam.business.documents.entity.AttachmentEntity;

/**
 * Pairs an attachment with a human-readable bookmark label used when building a merged PDF.
 * The label is derived from the entity context that owns the attachment (instrumentation name +
 * part label, display name, etc.) and is set by the caller that still has that context available.
 */
public record MergedPdfEntry(AttachmentEntity attachment, String bookmarkLabel) {}
