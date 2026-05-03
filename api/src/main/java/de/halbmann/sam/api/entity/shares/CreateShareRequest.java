package de.halbmann.sam.api.entity.shares;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

/**
 * Request body for creating a new share link.
 *
 * <p>The created share token (UUID) serves as the public link identifier — no separate secret is
 * generated. Callers should treat the token as a capability and distribute it only to intended
 * recipients.
 */
@Data
public class CreateShareRequest {

    /** The kind of resource to share (single instrumentation part or full collection). */
    @NotNull
    private ShareType resourceType;

    /** Primary key of the resource to share. Must match the given {@code resourceType}. */
    @NotNull
    private UUID resourceId;

    /** Optional expiry timestamp. {@code null} means the link never expires. */
    private OffsetDateTime expiresAt;
}
