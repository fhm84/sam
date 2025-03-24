package de.halbmann.sam.api.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class Genre {

    /**
     * The name of the genre.
     */
    @NotBlank
    String name;

}
