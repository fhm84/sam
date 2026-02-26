package de.halbmann.sam.api.entity;

import jakarta.json.bind.annotation.JsonbTypeAdapter;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
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

    /**
     * Unique identifier of the music sheet/piece.
     */
    UUID id;

    /**
     * Identifier (e.g. the number within the collection or booklet)
     */
    @NotBlank
    String identifier;

    UUID sheetId;

    /**
     * The title of the music sheet/piece.
     */
    String title;

    /**
     * (Optional) Subtitle of the piece.
     */
    String subtitle;

    Genre genre;

    Style style;

    @JsonbTypeAdapter(DurationJsonbAdapter.class)
    Duration duration;
}
