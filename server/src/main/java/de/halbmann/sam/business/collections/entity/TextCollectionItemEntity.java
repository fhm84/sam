package de.halbmann.sam.business.collections.entity;

import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

/** A collection item containing free-form text (e.g. an introductory note for a concert program). */
@Getter
@Setter
@Entity
@DiscriminatorValue("TEXT")
public class TextCollectionItemEntity extends CollectionItemEntity {

    @Column(columnDefinition = "text")
    String textContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    AttachmentEntity attachment;
}
