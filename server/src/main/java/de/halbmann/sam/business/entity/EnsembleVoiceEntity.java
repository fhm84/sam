package de.halbmann.sam.business.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.ArrayList;
import java.util.List;

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

    int minCount;
    int targetCount;
    int maxCount;

    @ManyToOne(optional = false)
    EnsembleEntity ensemble;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "voice", orphanRemoval = true)
    List<VoiceOptionEntity> options = new ArrayList<>();
}
