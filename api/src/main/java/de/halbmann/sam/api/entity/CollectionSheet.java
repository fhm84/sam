package de.halbmann.sam.api.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Wrapper entity for a sheet in context of a collection or a booklet, adding an order
 * number/identifier.
 */
@Data
@EqualsAndHashCode
public class CollectionSheet {

  /** Unique identifier of the music sheet/piece. */
  UUID id;

  /** Identifier (e.g. the number within the collection or booklet) */
  @NotBlank String identifier;

  @NotNull @Valid SheetMusic sheetMusic;
}
