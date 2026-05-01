package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.EventLogResource;
import de.halbmann.sam.api.entity.eventlog.EventLogEntry;
import de.halbmann.sam.api.entity.eventlog.EventLogFilterRequest;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.business.eventlog.controller.EventLogService;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@Authenticated
@RequestScoped
public class EventLogResourceImpl implements EventLogResource {

    @Inject
    EventLogService eventLogService;

    @Override
    public PaginatedResponse<EventLogEntry> find(EventLogFilterRequest filterRequest) {
        return eventLogService.find(filterRequest);
    }
}
