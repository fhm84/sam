package de.halbmann.sam.business.entity;

import de.halbmann.sam.api.entity.ClassificationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Entity to store classification requests (for documents uploaded without any metadata that are
 * therefore not linked to an instrumentation entity).
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "classifications")
public class Classification extends AbstractEntity {

    /**
     * The document to classify.
     */
    @OneToOne(fetch = FetchType.LAZY)
    AttachmentEntity attachment;

    /**
     * Status of the classification
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    ClassificationStatus status;
}
