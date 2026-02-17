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
@Table(name = "ensembles")
public class EnsembleEntity extends AbstractEntity {

    @NotBlank
    String name;

    @Column(columnDefinition = "text")
    String description;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "ensemble", orphanRemoval = true)
    List<EnsembleVoiceEntity> voices = new ArrayList<>();
}
