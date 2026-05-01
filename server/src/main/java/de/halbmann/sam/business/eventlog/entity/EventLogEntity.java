package de.halbmann.sam.business.eventlog.entity;

import de.halbmann.sam.api.entity.eventlog.EventType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "event_log")
public class EventLogEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    UUID id;

    OffsetDateTime occurredAt;

    String userId;

    String username;

    UUID shareTokenId;

    @Enumerated(EnumType.STRING)
    EventType eventType;

    String entityType;

    UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    Map<String, Object> metadata;
}
