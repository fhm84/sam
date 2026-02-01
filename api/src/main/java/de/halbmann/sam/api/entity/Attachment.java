package de.halbmann.sam.api.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An attachment (can be added to either SheetMusic or Instrumentation). This represents a file or
 * an external link.
 */
@Data
@EqualsAndHashCode
public class Attachment {

    /**
     * (Technical) Unique identifier
     */
    UUID id;

    /**
     * Human-readable name for the attachment.
     */
    String displayName;

    /**
     * The type of attachment, defined by the AttachmentType enum.
     */
    AttachmentType type;

    /**
     * The MIME type of the attachment.
     */
    String mimeType;

    /**
     * Size of the file in bytes.
     */
    long fileSize;

    /**
     * Checksum for file integrity verification (sha256)
     */
    String checksum;

    /**
     * Timestamp of when the attachment was uploaded or updated.
     */
    LocalDateTime uploadedAt;
}
