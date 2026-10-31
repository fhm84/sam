package de.halbmann.sam.api.impl;

import de.halbmann.sam.api.boundary.MyEventLogResource;
import de.halbmann.sam.api.entity.eventlog.EventLogEntry;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.business.eventlog.controller.EventLogService;
import de.halbmann.sam.security.CurrentUserService;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@Authenticated
@RequestScoped
public class MyEventLogResourceImpl implements MyEventLogResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    EventLogService eventLogService;

    @Override
    public PaginatedResponse<EventLogEntry> find(PaginationRequest pagination) {
        return eventLogService.findOwn(currentUserService.getUserId(), pagination);
    }
}
