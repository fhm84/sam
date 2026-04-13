package de.halbmann.sam.business.shared.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractEntity extends AbstractBaseEntity {

    @Id
    @GeneratedValue
    UUID id;
}
