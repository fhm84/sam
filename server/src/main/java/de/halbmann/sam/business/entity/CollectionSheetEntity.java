package de.halbmann.sam.business.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/** Wrapper entity for a sheet in context of a collection, adding an order number/identifier. */
@Getter
@Setter
@Entity
@Audited
@Table(name = "collection_sheets")
public class CollectionSheetEntity extends AbstractEntity {

  /** Identifier (e.g. the number within the collection) */
  String identifier;

  @ManyToOne SheetMusicEntity sheet;
}
