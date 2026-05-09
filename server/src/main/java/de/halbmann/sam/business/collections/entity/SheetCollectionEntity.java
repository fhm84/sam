package de.halbmann.sam.business.collections.entity;

import de.halbmann.sam.api.entity.collections.CollectionType;
import de.halbmann.sam.business.shared.entity.AbstractEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

    /**
     * The name to find the collection (like e.g. "Blaue Mappe", or "Programm Dorfplatzfest")
     */
    String name;

    /**
     * (Optional) description for the sheet collection
     */
    String description;

    /**
     * The type of the collection (a folder or a program/setlist)
     */
    @Enumerated(EnumType.STRING)
    CollectionType type;

    /**
     * (Optional) date of the collection (e.g. in case of a program for a gig)
     */
    LocalDate date;

    /**
     * The ordered list of items in this collection (sheet references and/or free-text blocks).
     */
    @OneToMany
    @JoinTable(
            name = "sheet_collections_items",
            joinColumns = @JoinColumn(name = "sheet_collections_id"),
            inverseJoinColumns = @JoinColumn(name = "items_id"))
    @OrderColumn(name = "items_order")
    List<CollectionItemEntity> items = new ArrayList<>();
}
