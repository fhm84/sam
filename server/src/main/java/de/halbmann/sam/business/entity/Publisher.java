package de.halbmann.sam.business.entity;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Stores information about publishers.
 */
@Getter
@Setter
@Entity
@Audited
@Cacheable
@Table(name = "publishers")
public class Publisher extends AbstractEntity {

    /**
     * Name
     */
    String name;
    /**
     * Contact
     */
    String contact;
    /**
     * Address
     */
    String address;

}
