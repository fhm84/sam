package de.halbmann.sam.business.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractEntity implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Id @GeneratedValue UUID id;

  @Version int version;

  LocalDateTime created;

  LocalDateTime lastUpdate;

  @PrePersist
  public void onCreate() {
    lastUpdate = created = LocalDateTime.now();
  }

  @PreUpdate
  public void onUpdate() {
    lastUpdate = LocalDateTime.now();
  }
}
