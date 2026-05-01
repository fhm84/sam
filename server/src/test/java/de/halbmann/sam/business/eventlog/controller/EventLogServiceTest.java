package de.halbmann.sam.business.eventlog.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import de.halbmann.sam.api.entity.eventlog.EventLogEntry;
import de.halbmann.sam.api.entity.eventlog.EventLogFilterRequest;
import de.halbmann.sam.api.entity.eventlog.EventType;
import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.business.eventlog.boundary.EventLogRepository;
import de.halbmann.sam.business.eventlog.entity.EventLogEntity;
import de.halbmann.sam.security.CurrentUserService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventLogServiceTest {

    @Mock
    EventLogRepository repository;

    @Mock
    CurrentUserService currentUserService;

    @Mock
    EventLogMapper eventLogMapper;

    @InjectMocks
    EventLogService service;

    // ── log() ──────────────────────────────────────────────────────────────

    @Test
    void log_populatesAllFields() {
        UUID entityId = UUID.randomUUID();
        Map<String, Object> metadata = Map.of("filename", "test.pdf");
        when(currentUserService.getUserId()).thenReturn("user-123");
        when(currentUserService.getUsername()).thenReturn("john.doe");

        service.log(EventType.DOCUMENT_DOWNLOAD, "document", entityId, metadata);

        ArgumentCaptor<EventLogEntity> captor = ArgumentCaptor.forClass(EventLogEntity.class);
        verify(repository).persist(captor.capture());
        EventLogEntity saved = captor.getValue();

        assertNotNull(saved.getOccurredAt());
        assertEquals("user-123", saved.getUserId());
        assertEquals("john.doe", saved.getUsername());
        assertEquals(EventType.DOCUMENT_DOWNLOAD, saved.getEventType());
        assertEquals("document", saved.getEntityType());
        assertEquals(entityId, saved.getEntityId());
        assertEquals(metadata, saved.getMetadata());
    }

    @Test
    void log_setsOccurredAtToNow() {
        OffsetDateTime before = OffsetDateTime.now();
        when(currentUserService.getUserId()).thenReturn(null);

        service.log(EventType.DOCUMENT_BATCH_DOWNLOAD, null, null, null);

        ArgumentCaptor<EventLogEntity> captor = ArgumentCaptor.forClass(EventLogEntity.class);
        verify(repository).persist(captor.capture());
        OffsetDateTime occurredAt = captor.getValue().getOccurredAt();

        assertFalse(occurredAt.isBefore(before));
        assertFalse(occurredAt.isAfter(OffsetDateTime.now()));
    }

    @Test
    void log_nullUserIdAndUsernameWhenAnonymous() {
        when(currentUserService.getUserId()).thenReturn(null);
        when(currentUserService.getUsername()).thenReturn(null);

        service.log(EventType.DOCUMENT_DOWNLOAD, "document", UUID.randomUUID(), null);

        ArgumentCaptor<EventLogEntity> captor = ArgumentCaptor.forClass(EventLogEntity.class);
        verify(repository).persist(captor.capture());
        assertNull(captor.getValue().getUserId());
        assertNull(captor.getValue().getUsername());
    }

    // ── find() ─────────────────────────────────────────────────────────────

    @Test
    void find_mapsDtoFields() {
        UUID id = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> metadata = Map.of("count", 3);

        EventLogEntity entity = new EventLogEntity();
        entity.setId(id);
        entity.setOccurredAt(now);
        entity.setUserId("user-42");
        entity.setEventType(EventType.DOCUMENT_BATCH_DOWNLOAD);
        entity.setEntityType("sheet");
        entity.setEntityId(entityId);
        entity.setMetadata(metadata);

        EventLogEntry expectedDto = new EventLogEntry();
        expectedDto.setId(id);
        expectedDto.setOccurredAt(now);
        expectedDto.setUserId("user-42");
        expectedDto.setEventType(EventType.DOCUMENT_BATCH_DOWNLOAD);
        expectedDto.setEntityType("sheet");
        expectedDto.setEntityId(entityId);
        expectedDto.setMetadata(metadata);

        when(repository.find(any())).thenReturn(new EventLogRepository.PagedResult(List.of(entity), 1L));
        when(eventLogMapper.toDto(entity)).thenReturn(expectedDto);

        PaginatedResponse<EventLogEntry> response = service.find(new EventLogFilterRequest());

        assertEquals(1, response.getData().size());
        EventLogEntry dto = response.getData().get(0);
        assertEquals(id, dto.getId());
        assertEquals(now, dto.getOccurredAt());
        assertEquals("user-42", dto.getUserId());
        assertEquals(EventType.DOCUMENT_BATCH_DOWNLOAD, dto.getEventType());
        assertEquals("sheet", dto.getEntityType());
        assertEquals(entityId, dto.getEntityId());
        assertEquals(metadata, dto.getMetadata());
    }

    @Test
    void find_emptyResult() {
        when(repository.find(any())).thenReturn(new EventLogRepository.PagedResult(List.of(), 0L));

        PaginatedResponse<EventLogEntry> response = service.find(new EventLogFilterRequest());

        assertEquals(0, response.getData().size());
        assertEquals(0L, response.getTotalCount());
    }

    @Test
    void find_paginationMetadata() {
        EventLogFilterRequest filter = new EventLogFilterRequest();
        filter.setPage(2);
        filter.setSize(10);
        when(repository.find(any())).thenReturn(new EventLogRepository.PagedResult(List.of(), 47L));

        PaginatedResponse<EventLogEntry> response = service.find(filter);

        assertEquals(2, response.getPage());
        assertEquals(47L, response.getTotalCount());
    }
}
