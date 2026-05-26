package de.halbmann.sam.business.musicians.entity;

import de.halbmann.sam.api.entity.musicians.MusicianRole;
import de.halbmann.sam.api.entity.musicians.MusicianStatus;
import de.halbmann.sam.business.shared.entity.AbstractEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

/**
 * Stores information about musicians (composers and arrangers).
 */
@Getter
@Setter
@Entity
@Audited
@Cacheable
@Table(name = "musicians")
public class MusicianEntity extends AbstractEntity {

    String name;

    Integer birthYear;

    Integer deathYear;

    String ipi;

    String email;

    String mobile;

    @Column(columnDefinition = "text")
    String notes;

    @Enumerated(EnumType.STRING)
    MusicianStatus status;

    @Enumerated(EnumType.STRING)
    MusicianRole role;

    @OneToMany(mappedBy = "musician", cascade = CascadeType.ALL, orphanRemoval = true)
    List<MusicianInstrumentEntity> instruments = new ArrayList<>();

    LocalDateTime lastInviteSentAt;

    @Column(unique = true)
    String userId;
}
