package de.halbmann.sam.api.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * Wrapper entity for a sheet in context of a collection, adding an order number/identifier.
 */
@Data
@EqualsAndHashCode
public class CollectionSheet {

    /**
     * Unique identifier of the music sheet/piece.
     */
    UUID id;
    /**
     * Identifier (e.g. the number within the collection)
     */
    String identifier;

    SheetMusic sheetMusic;

}
