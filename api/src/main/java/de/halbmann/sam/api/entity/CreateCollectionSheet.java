package de.halbmann.sam.api.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DTO for creating/adding a sheet in context of a collection or a booklet, adding an order number/identifier.
 */
@Data
@EqualsAndHashCode
public class CreateCollectionSheet {

    /**
     * Identifier (e.g. the number within the collection or booklet)
     */
    @NotBlank
    String identifier;

    /**
     * Unique identifier of the music sheet/piece.
     */
    @NotNull
    UUID sheetId;
}
