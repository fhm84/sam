package de.halbmann.sam.business.entity;

import de.halbmann.sam.api.entity.InstrumentTransposing;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Getter
@Setter
@Entity
@Audited
@Cacheable
@Table(name = "instruments")
public class InstrumentEntity extends AbstractBaseEntity {

    @Id
    String id;

    @NotBlank
    String name;

    String displayName;

    @Enumerated(EnumType.STRING)
    InstrumentTransposing transposition;
}
