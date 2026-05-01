package de.halbmann.sam.api.entity.eventlog;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

/**
 * A single entry in the event log representing one recorded business action. Complements the
 * Hibernate Envers audit trail by capturing read-side events (access, downloads) that do not
 * mutate data and are therefore not tracked by Envers.
 */
@Data
public class EventLogEntry {

    /** Unique identifier of this log entry. */
    private UUID id;

    /** Timestamp (with timezone) at which the event occurred. */
    private OffsetDateTime occurredAt;

    /** OIDC subject claim of the authenticated user, or {@code null} for anonymous access. */
    private String userId;

    /** Display name of the user at the time of the event ({@code preferred_username} claim). */
    private String username;

    /** ID of the share token used for this access, or {@code null} for authenticated requests. */
    private UUID shareTokenId;

    /** The type of event that was recorded. */
    private EventType eventType;

    /**
     * Logical name of the entity involved, e.g. {@code "document"}, {@code "sheet"},
     * {@code "instrumentation"}. {@code null} when the event is not tied to a specific entity.
     */
    private String entityType;

    /** Primary key of the affected entity, or {@code null} when not applicable. */
    private UUID entityId;

    /**
     * Event-type-specific details, e.g. {@code {"filename": "march.pdf"}} for a single download
     * or {@code {"count": 5, "format": "ZIP"}} for a batch download.
     */
    private Map<String, Object> metadata;
}
