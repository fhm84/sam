package de.halbmann.sam.api.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode
public class Attachment {

    /**
     * (Technical) Unique identifier
     */
    UUID id;
    /**
     * Unique identifier for the document.
     */
    String docIdentifier;
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
     * Checksum for file integrity verification.
     */
    long checksum;

    /**
     * Timestamp of when the attachment was uploaded or updated.
     */
    LocalDateTime uploadedAt;

}
