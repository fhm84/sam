package de.halbmann.sam.business.collections.entity;

import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

/** A collection item that references a piece of sheet music. */
@Getter
@Setter
@Entity
@DiscriminatorValue("SHEET")
public class SheetCollectionItemEntity extends CollectionItemEntity {

    @ManyToOne
    SheetMusicEntity sheet;
}
