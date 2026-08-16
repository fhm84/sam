package de.halbmann.sam.assistant.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.halbmann.sam.api.entity.ensembles.CoverageSnapshotSummary;
import de.halbmann.sam.api.entity.ensembles.CoverageStatus;
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
    void excludesIncompleteCoverage_includesCompleteAndPlayable() {
        when(context.getEnsembleId()).thenReturn(ensembleId);
        SheetMusicEntity complete = sheet("Alpha March");
        SheetMusicEntity playable = sheet("Beta Overture");
        SheetMusicEntity incomplete = sheet("Gamma Waltz");
        when(sheetRepository.findAiAssistantCandidates(null, null)).thenReturn(List.of(complete, playable, incomplete));
        when(coverageSnapshotService.findSummaries(eq(ensembleId), anyList()))
                .thenReturn(Map.of(
                        complete.getId(), summary(CoverageStatus.COMPLETE),
                        playable.getId(), summary(CoverageStatus.PLAYABLE),
                        incomplete.getId(), summary(CoverageStatus.INCOMPLETE)));

        String result = tool.searchRepertoire("", "", "");

        assertTrue(result.contains("Alpha March"));
        assertTrue(result.contains("Beta Overture"));
        assertFalse(result.contains("Gamma Waltz"));
    }

    @Test
    void noPlayableMatches_returnsFriendlyMessage() {
        when(context.getEnsembleId()).thenReturn(ensembleId);
        when(sheetRepository.findAiAssistantCandidates(null, null)).thenReturn(List.of());
        when(coverageSnapshotService.findSummaries(eq(ensembleId), anyList())).thenReturn(Map.of());

        String result = tool.searchRepertoire("", "", "");

        assertEquals("No playable sheets matched these filters.", result);
    }

    @Test
    void tagFilter_excludesSheetsWithoutMatchingTag() {
        when(context.getEnsembleId()).thenReturn(ensembleId);
        SheetMusicEntity tagged = sheet("Tagged Piece");
        tagged.setTags(Set.of("christmas"));
        SheetMusicEntity untagged = sheet("Untagged Piece");
        when(sheetRepository.findAiAssistantCandidates(null, null)).thenReturn(List.of(tagged, untagged));
        when(coverageSnapshotService.findSummaries(eq(ensembleId), anyList()))
                .thenReturn(Map.of(
                        tagged.getId(), summary(CoverageStatus.COMPLETE),
                        untagged.getId(), summary(CoverageStatus.COMPLETE)));

        String result = tool.searchRepertoire("", "", "christmas");

        assertTrue(result.contains("Tagged Piece"));
        assertFalse(result.contains("Untagged Piece"));
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
