package de.halbmann.sam.api.entity.collections;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * DTO for adding an item to a collection — either a sheet reference or a free-text block.
 */
@Data
@EqualsAndHashCode
public class CreateCollectionItem {

    @NotNull
    CollectionItemType type;

    String identifier;

    /** Required when type = SHEET. */
    UUID sheetId;

    /** Required when type = TEXT. */
    String textContent;
}
