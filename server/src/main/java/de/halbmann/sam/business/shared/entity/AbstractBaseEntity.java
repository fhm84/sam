package de.halbmann.sam.business.shared.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.Setter;

/**
 * Defines very basic abstract entity just adding a version for optimistic locking and creation and update timestamps.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractBaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Version
    int version;

    LocalDateTime created;

    LocalDateTime lastUpdate;

    @PrePersist
    public void onCreate() {
        lastUpdate = created = LocalDateTime.now(ZoneOffset.UTC);
    }

    @PreUpdate
    public void onUpdate() {
        lastUpdate = LocalDateTime.now(ZoneOffset.UTC);
    }
}
