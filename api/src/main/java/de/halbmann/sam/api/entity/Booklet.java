package de.halbmann.sam.api.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
public class Booklet {

    /**
     * Unique identifier of the booklet
     */
    UUID id;

    /**
     * The name to find the booklet (like e.g. "27 Weihnachtslieder", or "Gotteslob")
     */
    @NotBlank
    String name;

    /**
     * (Optional) description for the booklet
     */
    String description;

    /**
     * The list of sheets (wrapped in a collection sheet adding an identifier in context of the
     * collection/booklet)
     */
    @Valid
    List<CollectionSheet> sheets = new ArrayList<>();
}
