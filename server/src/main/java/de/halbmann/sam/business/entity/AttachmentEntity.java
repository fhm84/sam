package de.halbmann.sam.business.entity;

import de.halbmann.sam.api.entity.AttachmentType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Represents an attachment entity with metadata and auditing capabilities. This entity is mapped to
 * the "attachments" table in the database and extends the AbstractEntity class to inherit common
 * entity properties.
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "attachments")
public class AttachmentEntity extends AbstractEntity {

  /** Human-readable name for the attachment. */
  String displayName;

  /** The type of attachment, defined by the AttachmentType enum. */
  @Enumerated(EnumType.STRING)
  AttachmentType type;

  /** optional link to physical storage */
  @ManyToOne
  @JoinColumn(name = "document_id")
  private DocumentEntity document;

  /** Timestamp of when the attachment was uploaded or updated. */
  LocalDateTime uploadedAt;

  String uploadedBy;

  @PrePersist
  @PreUpdate
  void updateUploaded() {
    uploadedAt = LocalDateTime.now(ZoneOffset.UTC);
  }

  // FIXME: auto-"apply" uploadedBy!?

}
