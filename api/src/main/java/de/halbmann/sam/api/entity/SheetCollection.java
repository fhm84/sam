package de.halbmann.sam.api.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Groups multiple pieces of sheet music. */
@Data
@EqualsAndHashCode
public class SheetCollection {

  /** Unique identifier of the collection */
  UUID id;

  /** The name to find the collection (like e.g. "Blaue Mappe", or "Programm Dorfplatzfest") */
  @NotBlank String name;

  /** (Optional) description for the sheet collection */
  String description;

  /** (Optional) date of the collection (e.g. in case of a program for a gig) */
  LocalDate date;

  /**
   * The list of sheets (wrapped in a collection sheet adding an identifier in context of the
   * collection)
   */
  @Valid List<CollectionSheet> sheets = new ArrayList<>();
}
