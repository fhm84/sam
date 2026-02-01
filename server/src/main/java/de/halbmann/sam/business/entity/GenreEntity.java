package de.halbmann.sam.business.entity;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Getter
@Setter
@Entity
@Audited
@Cacheable
@Table(name = "genres")
public class GenreEntity extends AbstractEntity {

    /**
     * The name of the genre.
     */
    @Column(unique = true)
    String name;
}
