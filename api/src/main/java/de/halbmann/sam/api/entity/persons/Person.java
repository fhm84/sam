package de.halbmann.sam.api.entity.persons;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A person credited on sheets (composer, arranger), whether external, historical or a band member.
 * Holds only what is printed on the music; a band member's personal data is in their
 * {@link de.halbmann.sam.api.entity.members.Member} profile, which shares this ID.
 */
@Data
@EqualsAndHashCode
public class Person {

    UUID id;

    @NotBlank
    String name;

    Integer birthYear;

    Integer deathYear;

    /**
     * Interested Party Information (IPI)-Number
     */
    String ipi;
}
