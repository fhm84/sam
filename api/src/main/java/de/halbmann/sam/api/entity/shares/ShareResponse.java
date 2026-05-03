package de.halbmann.sam.api.entity.shares;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

/**
 * Represents a share link as returned to the authenticated creator.
 *
 * <p>The {@code id} field doubles as the public share token: the shareable URL is
 * {@code /share/{id}}. Only the creator can list or revoke their own shares.
 */
@Data
public class ShareResponse {

    /** The share token UUID, used directly as the public URL path segment. */
    private UUID id;

    private ShareType resourceType;

    /** Primary key of the shared resource. */
    private UUID resourceId;

    /**
     * Human-readable label for the shared resource, resolved at list time.
     * For {@code INSTRUMENTATION}: instrument name + optional part label (e.g. "Trumpet 1").
     * For {@code COLLECTION}: collection name.
     */
    private String resourceLabel;

    /** Expiry timestamp, or {@code null} if the link never expires. */
    private OffsetDateTime expiresAt;

    private OffsetDateTime createdAt;

    /** {@code true} once the share has been explicitly revoked by its creator. */
    private boolean revoked;
}
