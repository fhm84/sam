package de.halbmann.sam.api.entity.shares;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

/**
 * Represents a share link as returned to the authenticated creator.
 *
 * <p>The {@code id} field doubles as the public share token: the shareable URL is
 * {@code /share/{id}}. The creator lists and revokes their own shares; admins all of them.
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

    /** {@code true} once the share has been explicitly revoked (by its creator or an admin). */
    private boolean revoked;

    /**
     * Who created the link: the name of the member their login is linked to. Only in the admins'
     * list of all shares, and {@code null} when the login isn't linked (e.g. the {@code cli}).
     */
    private String creatorName;
}
