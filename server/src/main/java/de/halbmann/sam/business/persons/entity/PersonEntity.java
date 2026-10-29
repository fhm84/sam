package de.halbmann.sam.business.persons.entity;

import de.halbmann.sam.business.shared.entity.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * A person credited on sheets (composer, arranger). Band members are persons too; their personal
 * data lives in their member profile, which shares this ID (ADR-0015).
 */
@Getter
@Setter
@Entity
@Audited
@Cacheable
@Table(name = "persons")
public class PersonEntity extends AbstractEntity {

    String name;

    Integer birthYear;

    Integer deathYear;

    String ipi;
}
