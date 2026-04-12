package de.halbmann.sam.api.entity;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

    /**
     * OIDC subject claim linking this musician to an authenticated user account.
     * Null for external/historical musicians (composers, arrangers) with no system login.
     */
    String userId;
}
