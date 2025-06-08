package de.halbmann.sam.business.entity;

import de.halbmann.sam.api.entity.AttachmentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Represents an attachment entity with metadata and auditing capabilities.
 * This entity is mapped to the "attachments" table in the database and
 * extends the AbstractEntity class to inherit common entity properties.
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "attachments")
public class AttachmentEntity extends AbstractEntity {

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
    @Enumerated(EnumType.STRING)
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
     * Path to the file's storage location.
     */
    String referencePath;
    /**
     * Checksum for file integrity verification.
     */
    long checksum;

    /**
     * Timestamp of when the attachment was uploaded or updated.
     */
    LocalDateTime uploadedAt;

    @PrePersist
    @PreUpdate
    void updateUploaded() {
        uploadedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

}
