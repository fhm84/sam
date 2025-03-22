package de.halbmann.sam.business.entity;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Stores information about musicians (composers and arrangers).
 */
@Getter
@Setter
@Entity
@Audited
@Cacheable
@Table(name = "musicians")
public class MusicianEntity extends AbstractEntity {

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
