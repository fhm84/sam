package de.halbmann.sam.business.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/** Represents a document (physical storage) entity with metadata and auditing capabilities. */
@Getter
@Setter
@Entity
@Audited
@Table(name = "documents", uniqueConstraints = @UniqueConstraint(columnNames = "sha256"))
public class DocumentEntity extends AbstractEntity {

  @Column(nullable = false)
  private String filename;

  @Column(nullable = false)
  private String path;

  @Column(nullable = false)
  private long size;

  /** The MIME type of the document. */
  private String mimeType;

  @Column(length = 64, nullable = false, unique = true)
  private String sha256;

  @Column(nullable = false)
  private int refCount = 1;
}
