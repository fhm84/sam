package de.halbmann.sam.business.eventlog.controller;

import de.halbmann.sam.api.entity.eventlog.EventLogEntry;
import de.halbmann.sam.api.entity.eventlog.EventLogFilterRequest;
import de.halbmann.sam.api.entity.eventlog.EventType;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.business.eventlog.boundary.EventLogRepository;
import de.halbmann.sam.business.eventlog.entity.EventLogEntity;
import de.halbmann.sam.security.CurrentUserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class EventLogService {

    @Inject
    EventLogRepository repository;

    @Inject
    CurrentUserService currentUserService;

    @Inject
    EventLogMapper eventLogMapper;

    @Transactional
    public void log(EventType eventType, String entityType, UUID entityId, Map<String, Object> metadata) {
        log(eventType, entityType, entityId, metadata, null);
    }

    @Transactional
    public void log(
            EventType eventType, String entityType, UUID entityId, Map<String, Object> metadata, UUID shareTokenId) {
        EventLogEntity entry = new EventLogEntity();
        entry.setOccurredAt(OffsetDateTime.now());
        entry.setUserId(shareTokenId == null ? currentUserService.getUserId() : null);
        entry.setUsername(shareTokenId == null ? currentUserService.getUsername() : null);
        entry.setShareTokenId(shareTokenId);
        entry.setEventType(eventType);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setMetadata(metadata);
        repository.persist(entry);
    }

    @Transactional
    public PaginatedResponse<EventLogEntry> find(EventLogFilterRequest filter) {
        EventLogRepository.PagedResult result = repository.find(filter);

        PaginatedResponse<EventLogEntry> response = new PaginatedResponse<>();
        response.setData(result.data().stream().map(eventLogMapper::toDto).toList());
        response.setPage(filter.getPage());
        response.setSize(response.getData().size());
        response.setTotalCount(result.totalCount());
        return response;
    }
}
