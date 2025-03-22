package de.halbmann.sam.business.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Getter
@Setter
@Entity
@Audited
@Table(name = "documents")
public class Document extends AbstractEntity {

    String docIdentifier;
    String displayName;
    String mimeType;
    long fileSize;
    String referencePath;
    long checksum;

}
