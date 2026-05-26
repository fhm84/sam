package de.halbmann.sam.business.instruments.entity;

import de.halbmann.sam.api.entity.instruments.Clef;
import de.halbmann.sam.api.entity.instruments.InstrumentFamily;
import de.halbmann.sam.api.entity.instruments.InstrumentTransposing;
import de.halbmann.sam.business.shared.entity.AbstractBaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
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
    InstrumentFamily family;

    @Enumerated(EnumType.STRING)
    Clef defaultClef;

    String catalogSection;

    Integer catalogPosition;

    @ElementCollection
    @CollectionTable(name = "instrument_aliases", joinColumns = @JoinColumn(name = "instrument_id"))
    @Column(name = "alias")
    @OrderColumn(name = "alias_order")
    List<String> aliases = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    InstrumentTransposing transposition;
}
