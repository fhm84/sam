package de.halbmann.sam.assistant.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.halbmann.sam.api.entity.ensembles.CoverageSnapshotSummary;
import de.halbmann.sam.api.entity.ensembles.CoverageStatus;
import de.halbmann.sam.api.entity.sheets.Genre;
import de.halbmann.sam.business.ensembles.controller.CoverageSnapshotService;
import de.halbmann.sam.business.musicians.entity.MusicianEntity;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetlistCandidateToolTest {

    @Mock
    SheetRepository sheetRepository;

    @Mock
    CoverageSnapshotService coverageSnapshotService;

    @Mock
    SetlistAssistantContext context;

    @InjectMocks
    SetlistCandidateTool tool;

    final UUID ensembleId = UUID.randomUUID();

    @Test
    void noEnsembleConfigured_returnsMessage_withoutQueryingRepositories() {
        when(context.getEnsembleId()).thenReturn(null);

        String result = tool.searchRepertoire("", "", "");

        assertTrue(result.contains("No ensemble"));
        verifyNoInteractions(sheetRepository, coverageSnapshotService);
    }

    @Test
    void formatsCandidates_andRecordsServedSheetIds() {
        when(context.getEnsembleId()).thenReturn(ensembleId);
        SheetMusicEntity alpha = sheet("Alpha March");
        SheetMusicEntity beta = sheet("Beta Overture");
        when(sheetRepository.findAiAssistantCandidates(ensembleId, null, null, Set.of(), 20))
                .thenReturn(List.of(alpha, beta));
        when(coverageSnapshotService.findSummaries(eq(ensembleId), anyList()))
                .thenReturn(Map.of(
                        alpha.getId(), summary(CoverageStatus.COMPLETE),
                        beta.getId(), summary(CoverageStatus.PLAYABLE)));

        String result = tool.searchRepertoire("", "", "");

        assertTrue(result.contains("Alpha March"));
        assertTrue(result.contains("Beta Overture"));
        verify(context).recordServedSheetIds(List.of(alpha.getId(), beta.getId()));
    }

    @Test
    void noMatches_returnsFriendlyMessage_andRecordsNothing() {
        when(context.getEnsembleId()).thenReturn(ensembleId);
        when(sheetRepository.findAiAssistantCandidates(eq(ensembleId), isNull(), isNull(), eq(Set.of()), eq(20)))
                .thenReturn(List.of());

        String result = tool.searchRepertoire("", "", "");

        assertEquals("No playable sheets matched these filters.", result);
        verify(context, never()).recordServedSheetIds(anyList());
        verifyNoInteractions(coverageSnapshotService);
    }

    @Test
    void duplicateAndEmptyTagSegments_areDedupedAndDropped_withoutCrashing() {
        when(context.getEnsembleId()).thenReturn(ensembleId);
        when(sheetRepository.findAiAssistantCandidates(any(), any(), any(), anySet(), anyInt()))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> tool.searchRepertoire("", "", "march, march,,christmas"));

        verify(sheetRepository).findAiAssistantCandidates(ensembleId, null, null, Set.of("march", "christmas"), 20);
    }

    @Test
    void genreAndDurationFilters_areParsedAndPassedToTheQuery() {
        when(context.getEnsembleId()).thenReturn(ensembleId);
        when(sheetRepository.findAiAssistantCandidates(any(), any(), any(), anySet(), anyInt()))
                .thenReturn(List.of());

        tool.searchRepertoire("march", "10", "");

        verify(sheetRepository)
                .findAiAssistantCandidates(ensembleId, Genre.MARCH, Duration.ofMinutes(10), Set.of(), 20);
    }

    private SheetMusicEntity sheet(String title) {
        SheetMusicEntity s = new SheetMusicEntity();
        s.setId(UUID.randomUUID());
        s.setTitle(title);
        MusicianEntity composer = new MusicianEntity();
        composer.setName("Test Composer");
        s.setComposer(composer);
        s.setDuration(Duration.ofMinutes(4));
        return s;
    }

    private CoverageSnapshotSummary summary(CoverageStatus status) {
        CoverageSnapshotSummary summary = new CoverageSnapshotSummary();
        summary.setStatus(status);
        return summary;
    }
}
