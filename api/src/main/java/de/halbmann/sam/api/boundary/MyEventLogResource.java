package de.halbmann.sam.api.boundary;

import de.halbmann.sam.api.entity.eventlog.EventLogEntry;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/** The calling user's own event-log entries (GDPR right of access). */
@Path("me/event-logs")
@Produces(MediaType.APPLICATION_JSON)
public interface MyEventLogResource {

    /**
     * Returns the entries recorded under the caller's login, newest first. Entries made through a
     * share link and entries of other users are never included.
     *
     * @return a page of the caller's own entries; empty when the login has no user ID
     */
    @GET
    PaginatedResponse<EventLogEntry> find(@BeanParam PaginationRequest pagination);
}
