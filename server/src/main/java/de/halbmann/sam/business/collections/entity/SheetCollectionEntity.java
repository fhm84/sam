package de.halbmann.sam.business.collections.entity;

import de.halbmann.sam.api.entity.collections.CollectionType;
import de.halbmann.sam.api.entity.collections.CollectionVisibility;
import de.halbmann.sam.business.shared.entity.AbstractEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Groups multiple pieces of sheet music.
 */
@Getter
@Setter
@Entity
@Audited
@Table(name = "sheet_collections")
public class SheetCollectionEntity extends AbstractEntity {

    String name;

    String description;

    @Enumerated(EnumType.STRING)
    CollectionType type;

    LocalDate date;

    @Enumerated(EnumType.STRING)
    CollectionVisibility visibility;

    String coverColor;

    UUID coverImageId;

    /**
     * Which ensemble this setlist/collection belongs to. Used to evaluate coverage against
     * the correct ensemble (e.g. for the AI setlist assistant); {@code null} for collections
     * not tied to a specific ensemble.
     */
    UUID ensembleId;

    @OneToMany
    @JoinTable(
            name = "sheet_collections_items",
            joinColumns = @JoinColumn(name = "sheet_collections_id"),
            inverseJoinColumns = @JoinColumn(name = "items_id"))
    @OrderColumn(name = "items_order")
    List<CollectionItemEntity> items = new ArrayList<>();
}
