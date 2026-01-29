package de.halbmann.sam.business.entity;

import de.halbmann.sam.api.entity.CollectionType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/** Groups multiple pieces of sheet music. */
@Getter
@Setter
@Entity
@Audited
@Table(name = "sheet_collections")
public class SheetCollectionEntity extends AbstractEntity {

  /** The name to find the collection (like e.g. "Blaue Mappe", or "Programm Dorfplatzfest") */
  String name;

  /** (Optional) description for the sheet collection */
  String description;

  /** The type of the collection (a folder or a program/setlist) */
  @Enumerated(EnumType.STRING)
  CollectionType type;

  /** (Optional) date of the collection (e.g. in case of a program for a gig) */
  LocalDate date;

  /**
   * The list of sheets (wrapped in a collection sheet adding an identifier in context of the
   * collection)
   */
  @OneToMany List<CollectionSheetEntity> sheets = new ArrayList<>();
}
