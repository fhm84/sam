package de.halbmann.sam.business.musicians.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.business.musicians.boundary.MusicianRepository;
import de.halbmann.sam.business.musicians.entity.MusicianEntity;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MusicianServiceTest {

    @Mock
    MusicianRepository musicianRepository;

    @Mock
    MusicianMapper musicianMapper;

    @InjectMocks
    MusicianService service;

    // ── linkUser ──────────────────────────────────────────────────────────

    @Test
    void linkUser_setsUserIdOnEntity() {
        UUID id = UUID.randomUUID();
        MusicianEntity entity = new MusicianEntity();
        when(musicianRepository.findByIdOptional(id)).thenReturn(Optional.of(entity));

        service.linkUser(id.toString(), "user-abc");

        assertEquals("user-abc", entity.getUserId());
    }

    @Test
    void linkUser_unknownMusician_throwsEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(musicianRepository.findByIdOptional(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.linkUser(id.toString(), "user-abc"));
    }

    // ── unlinkUser ────────────────────────────────────────────────────────

    @Test
    void unlinkUser_clearsUserId() {
        UUID id = UUID.randomUUID();
        MusicianEntity entity = new MusicianEntity();
        entity.setUserId("user-abc");
        when(musicianRepository.findByIdOptional(id)).thenReturn(Optional.of(entity));

        service.unlinkUser(id.toString());

        assertNull(entity.getUserId());
    }

    @Test
    void unlinkUser_unknownMusician_throwsEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(musicianRepository.findByIdOptional(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.unlinkUser(id.toString()));
    }

    // ── updateMusician does NOT clear userId ──────────────────────────────

    @Test
    void updateMusician_doesNotClearUserId() {
        UUID id = UUID.randomUUID();
        MusicianEntity entity = new MusicianEntity();
        entity.setUserId("user-abc");
        when(musicianRepository.findByIdOptional(id)).thenReturn(Optional.of(entity));

        // Simulate what the mapper does: update name/ipi/years but leave userId alone
        // (MusicianMapper.update has @Mapping(target = "userId", ignore = true))
        Musician dto = new Musician();
        dto.setName("Updated Name");
        doAnswer(inv -> {
                    MusicianEntity e = inv.getArgument(0);
                    e.setName("Updated Name");
                    // userId intentionally not touched — mirrors the real mapper behaviour
                    return null;
                })
                .when(musicianMapper)
                .update(any(), any());

        service.updateMusician(id.toString(), dto);

        assertEquals(
                "user-abc",
                entity.getUserId(),
                "updateMusician must not clear the userId set by the dedicated linkUser endpoint");
    }
}
