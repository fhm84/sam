package de.halbmann.sam.business.shares.entity;

import de.halbmann.sam.api.entity.shares.ShareType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Persistent share token. The {@code id} UUID is the public token — it appears directly in share
 * URLs and is the only credential needed to access the shared resource. No separate secret exists.
 *
 * <p>A share becomes inactive when either {@code expiresAt} is in the past or {@code revokedAt}
 * is set. Both conditions are checked by {@link #isValid()}.
 */
@Getter
@Setter
@Entity
@Table(name = "shares")
public class ShareEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    UUID id;

    String creatorUserId;

    @Enumerated(EnumType.STRING)
    ShareType resourceType;

    UUID resourceId;

    OffsetDateTime expiresAt;

    OffsetDateTime revokedAt;

    OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(OffsetDateTime.now());
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }
}
