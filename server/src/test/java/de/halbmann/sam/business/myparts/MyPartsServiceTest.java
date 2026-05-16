package de.halbmann.sam.business.myparts;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.halbmann.sam.api.entity.shared.PaginatedResponse;
import de.halbmann.sam.api.entity.shared.PaginationRequest;
import de.halbmann.sam.api.entity.sheets.Instrumentation;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.api.entity.sheets.SheetWithMyParts;
import de.halbmann.sam.business.ensembles.boundary.EnsembleMembershipRepository;
import de.halbmann.sam.business.ensembles.entity.EnsembleMembershipEntity;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.business.musicians.boundary.MusicianRepository;
import de.halbmann.sam.business.musicians.entity.MusicianEntity;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.controller.InstrumentationMapper;
import de.halbmann.sam.business.sheets.controller.SheetMusicMapper;
import de.halbmann.sam.business.sheets.entity.InstrumentationEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.core.entity.PaginatedEntities;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyPartsServiceTest {

    @Mock
    MusicianRepository musicianRepository;

    @Mock
    EnsembleMembershipRepository membershipRepository;

    @Mock
    SheetRepository sheetRepository;

    @Mock
    SheetMusicMapper sheetMusicMapper;

    @Mock
    InstrumentationMapper instrumentationMapper;

    @InjectMocks
    MyPartsService service;

    // ── null userId ────────────────────────────────────────────────────────

    @Test
    void nullUserId_returnsEmpty_withoutRepoAccess() {
        PaginationRequest filter = new PaginationRequest();

        PaginatedResponse<SheetWithMyParts> result = service.getMyParts(null, filter);

        assertEquals(0, result.getData().size());
        assertEquals(0L, result.getTotalCount());
        verifyNoInteractions(musicianRepository, membershipRepository, sheetRepository);
    }

    // ── no linked musician ─────────────────────────────────────────────────

    @Test
    void noLinkedMusician_returnsEmpty() {
        when(musicianRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        PaginationRequest filter = new PaginationRequest();

        PaginatedResponse<SheetWithMyParts> result = service.getMyParts("user-1", filter);

        assertEquals(0, result.getData().size());
        verifyNoInteractions(membershipRepository, sheetRepository);
    }

    // ── conductor-only musician (no instrument) ────────────────────────────

    @Test
    void conductorOnly_noInstrument_returnsEmpty() {
        MusicianEntity musician = musician("user-2");
        when(musicianRepository.findByUserId("user-2")).thenReturn(Optional.of(musician));

        EnsembleMembershipEntity conductorMembership = new EnsembleMembershipEntity();
        conductorMembership.setMusician(musician);
        conductorMembership.setInstrument(null);

        when(membershipRepository.list(anyString(), anyMap())).thenReturn(List.of(conductorMembership));
        PaginationRequest filter = new PaginationRequest();

        PaginatedResponse<SheetWithMyParts> result = service.getMyParts("user-2", filter);

        assertEquals(0, result.getData().size());
        verifyNoInteractions(sheetRepository);
    }

    // ── matching instrumentations are included; non-matching are filtered ──

    @Test
    void matchingInstruments_returnsSheetWithFilteredInstrumentations() {
        MusicianEntity musician = musician("user-3");
        when(musicianRepository.findByUserId("user-3")).thenReturn(Optional.of(musician));

        InstrumentEntity trumpet = instrument("trumpet-bb");
        InstrumentEntity trombone = instrument("trombone");

        EnsembleMembershipEntity membership = new EnsembleMembershipEntity();
        membership.setMusician(musician);
        membership.setInstrument(trumpet);

        when(membershipRepository.list(anyString(), anyMap())).thenReturn(List.of(membership));

        InstrumentationEntity matchingPart = new InstrumentationEntity();
        matchingPart.setInstrument(trumpet);

        InstrumentationEntity nonMatchingPart = new InstrumentationEntity();
        nonMatchingPart.setInstrument(trombone);

        SheetMusicEntity sheet = new SheetMusicEntity();
        sheet.getInstrumentations().add(matchingPart);
        sheet.getInstrumentations().add(nonMatchingPart);

        PaginationRequest filter = new PaginationRequest();
        when(sheetRepository.findSheetsForInstruments(List.of("trumpet-bb"), 0, 10))
                .thenReturn(new PaginatedEntities<>(List.of(sheet), 1L));

        SheetMusic sheetDto = new SheetMusic();
        when(sheetMusicMapper.toDto(sheet)).thenReturn(sheetDto);

        Instrumentation instrDto = new Instrumentation();
        when(instrumentationMapper.toDto(matchingPart)).thenReturn(instrDto);

        PaginatedResponse<SheetWithMyParts> result = service.getMyParts("user-3", filter);

        assertEquals(1, result.getData().size());
        assertEquals(1L, result.getTotalCount());
        SheetWithMyParts item = result.getData().get(0);
        assertEquals(1, item.getMyInstrumentations().size());
        assertSame(instrDto, item.getMyInstrumentations().get(0));
        verify(instrumentationMapper, never()).toDto(nonMatchingPart);
    }

    // ── musician doubling two instruments — both parts returned ────────────

    @Test
    void doubling_bothInstrumentsIncluded() {
        MusicianEntity musician = musician("user-4");
        when(musicianRepository.findByUserId("user-4")).thenReturn(Optional.of(musician));

        InstrumentEntity trumpet = instrument("trumpet-bb");
        InstrumentEntity flugelhorn = instrument("flugelhorn");

        EnsembleMembershipEntity m1 = new EnsembleMembershipEntity();
        m1.setMusician(musician);
        m1.setInstrument(trumpet);

        EnsembleMembershipEntity m2 = new EnsembleMembershipEntity();
        m2.setMusician(musician);
        m2.setInstrument(flugelhorn);

        when(membershipRepository.list(anyString(), anyMap())).thenReturn(List.of(m1, m2));

        InstrumentationEntity trumpetPart = new InstrumentationEntity();
        trumpetPart.setInstrument(trumpet);

        InstrumentationEntity flugelhornPart = new InstrumentationEntity();
        flugelhornPart.setInstrument(flugelhorn);

        SheetMusicEntity sheet = new SheetMusicEntity();
        sheet.getInstrumentations().add(trumpetPart);
        sheet.getInstrumentations().add(flugelhornPart);

        PaginationRequest filter = new PaginationRequest();
        when(sheetRepository.findSheetsForInstruments(
                        argThat(ids -> ids.size() == 2 && ids.contains("trumpet-bb") && ids.contains("flugelhorn")),
                        eq(0),
                        eq(10)))
                .thenReturn(new PaginatedEntities<>(List.of(sheet), 1L));

        SheetMusic sheetDto = new SheetMusic();
        when(sheetMusicMapper.toDto(sheet)).thenReturn(sheetDto);
        when(instrumentationMapper.toDto(trumpetPart)).thenReturn(new Instrumentation());
        when(instrumentationMapper.toDto(flugelhornPart)).thenReturn(new Instrumentation());

        PaginatedResponse<SheetWithMyParts> result = service.getMyParts("user-4", filter);

        assertEquals(1, result.getData().size());
        assertEquals(2, result.getData().get(0).getMyInstrumentations().size());
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private MusicianEntity musician(String userId) {
        MusicianEntity m = new MusicianEntity();
        m.setUserId(userId);
        return m;
    }

    private InstrumentEntity instrument(String id) {
        InstrumentEntity i = new InstrumentEntity();
        i.setId(id);
        return i;
    }
}
