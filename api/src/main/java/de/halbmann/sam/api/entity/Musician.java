package de.halbmann.sam.api.entity;

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
     * Interested Party Information (IPI)-Nummer
     */
    String ipi;

}
