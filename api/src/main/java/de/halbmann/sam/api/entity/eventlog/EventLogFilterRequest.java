package de.halbmann.sam.api.entity.eventlog;

import de.halbmann.sam.api.entity.shared.PaginationRequest;
import jakarta.ws.rs.QueryParam;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Query parameters for filtering and paginating the event log. All filters are optional and
 * combined with AND when multiple are specified. Inherits {@code page}, {@code size}, and
 * {@code sortBy} from {@link PaginationRequest}.
 */
@Getter
@Setter
public class EventLogFilterRequest extends PaginationRequest {

    /** Restrict results to one or more event types (OR semantics within the list). */
    @QueryParam("eventTypes")
    private Set<EventType> eventTypes;

    /**
     * Restrict results to events involving a specific entity type, e.g. {@code "document"} or
     * {@code "sheet"}.
     */
    @QueryParam("entityType")
    private String entityType;

    /** Restrict results to events performed by a specific user (matched on OIDC subject). */
    @QueryParam("userId")
    private String userId;

    /** Restrict results to events triggered via a specific share token. */
    @QueryParam("shareTokenId")
    private UUID shareTokenId;
}
