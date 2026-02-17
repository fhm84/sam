package de.halbmann.sam.business.entity;

import de.halbmann.sam.api.entity.VoiceOptionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Getter
@Setter
@Entity
@Audited
@Cacheable
@Table(name = "voice_options")
public class VoiceOptionEntity extends AbstractEntity {

    @ManyToOne(optional = false)
    EnsembleVoiceEntity voice;

    @ManyToOne(optional = false)
    InstrumentEntity instrument;

    @Enumerated(EnumType.STRING)
    VoiceOptionType type;

    double factor;
}
