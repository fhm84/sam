package de.halbmann.sam.business.sheets.entity;

import de.halbmann.sam.api.entity.instruments.VoiceOptionType;
import de.halbmann.sam.business.ensembles.entity.EnsembleVoiceEntity;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.business.shared.entity.AbstractEntity;
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

    public static VoiceOptionEntity defaultOption() {
        VoiceOptionEntity o = new VoiceOptionEntity();
        o.setFactor(1.0);
        o.setInstrument(null); // bedeutet: match anything
        return o;
    }
}
