package de.halbmann.sam.api.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * Stores information about musicians (composers and arrangers).
 */
@Data
@EqualsAndHashCode
public class Musician {

    /**
     * Unique identifier for the musician
     */
    UUID id;

    /**
     * Full name
     */
    @NotBlank
    String name;

    /**
     * Year of birth
     */
    Integer birthYear;

    /**
     * Year of death
     */
    Integer deathYear;

    /**
     * Interested Party Information (IPI)-Number
     */
    String ipi;
}
