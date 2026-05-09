package de.halbmann.sam.business.collections.entity;

import de.halbmann.sam.business.shared.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Base entity for an ordered item within a collection. Concrete subtypes:
 * {@link SheetCollectionItemEntity} (SHEET) and {@link TextCollectionItemEntity} (TEXT).
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "collection_items")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_type", discriminatorType = DiscriminatorType.STRING)
public abstract class CollectionItemEntity extends AbstractEntity {

    String identifier;
}
