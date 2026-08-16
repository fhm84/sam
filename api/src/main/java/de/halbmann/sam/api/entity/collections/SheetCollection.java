package de.halbmann.sam.api.entity.collections;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Groups multiple pieces of sheet music.
 */
@Data
@EqualsAndHashCode
public class SheetCollection {

    UUID id;

    @NotBlank
    String name;

    String description;

    CollectionType type;

    LocalDate date;

    /**
     * Who can see this collection.
     */
    CollectionVisibility visibility;

    /**
     * Hex color string used as the collection cover background (e.g. "#3b82f6").
     */
    String coverColor;

    /**
     * Optional FK to a document used as the collection cover image.
     */
    UUID coverImageId;

    /**
     * Which ensemble this setlist/collection belongs to, or null if not tied to one.
     */
    UUID ensembleId;

    /**
     * The ordered list of items in the collection (sheet references and/or free-text blocks).
     */
    @Valid
    List<CollectionItem> items = new ArrayList<>();
}
