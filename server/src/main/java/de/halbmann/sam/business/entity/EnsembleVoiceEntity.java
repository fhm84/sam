package de.halbmann.sam.business.entity;

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
@Table(name = "ensemble_voices")
public class EnsembleVoiceEntity extends AbstractEntity {

    @NotBlank
    String label;

    double weight;

    boolean required;

    @ManyToOne(optional = false)
    EnsembleEntity ensemble;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "voice", orphanRemoval = true)
    List<VoiceOptionEntity> options = new ArrayList<>();
}
