package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.eventlog.EventLogEntry;
import de.halbmann.sam.api.entity.eventlog.EventLogFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * Read-only endpoint for querying the event log. The event log captures business-level access
 * events (e.g. document downloads) that are not covered by the Hibernate Envers change history.
 * Results are returned newest-first and support filtering by event type, entity, and user.
 */
@Path("event-logs")
@Produces(MediaType.APPLICATION_JSON)
public interface EventLogResource {

    /**
     * Returns a paginated, newest-first list of event log entries. All filter parameters are
     * optional; omitting them returns all recorded events.
     */
    @GET
    PaginatedResponse<EventLogEntry> find(@BeanParam EventLogFilterRequest filterRequest);
}
