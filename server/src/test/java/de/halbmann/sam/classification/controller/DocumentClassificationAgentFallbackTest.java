package de.halbmann.sam.classification.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.halbmann.sam.api.entity.classification.ClassificationApplyRequest;
import de.halbmann.sam.api.entity.classification.SheetClassification;
import de.halbmann.sam.business.documents.boundary.DocumentRepository;
import de.halbmann.sam.business.documents.entity.DocumentEntity;
import de.halbmann.sam.business.instruments.boundary.InstrumentRepository;
import de.halbmann.sam.business.instruments.entity.InstrumentEntity;
import de.halbmann.sam.business.musicians.boundary.MusicianRepository;
import de.halbmann.sam.business.musicians.entity.MusicianEntity;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.classification.boundary.ClassificationAgent;
import de.halbmann.sam.classification.boundary.ClassificationService;
import de.halbmann.sam.classification.entity.SheetAnalyzerResult;
import de.halbmann.storage.api.FileSystemWrapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * In agentic mode the agent's answer replaces the pre-filled suggestion, but an ID the agent
 * invented must not survive: an unknown sheet ID made apply fail with 404, and an unknown
 * composer/instrument ID silently dropped that composer/instrument.
 */
class DocumentClassificationAgentFallbackTest {

    private static final UUID DOCUMENT_ID = UUID.randomUUID();

    DocumentClassificationService service;

    ClassificationAgent agent;

    MusicianRepository musicianRepository;

    InstrumentRepository instrumentRepository;

    SheetRepository sheetRepository;

    @BeforeEach
    void setUp() throws Exception {
        service = new DocumentClassificationService();
        service.agenticMode = true;
        service.registry = new SimpleMeterRegistry();

        DocumentEntity doc = new DocumentEntity();
        doc.setId(DOCUMENT_ID);
        doc.setPath("ab/cd/ef/doc.png");
        doc.setMimeType("image/png");
        service.documentRepository = mock(DocumentRepository.class);
        when(service.documentRepository.findByIdOptional(DOCUMENT_ID)).thenReturn(Optional.of(doc));

        service.filesystem = mock(FileSystemWrapper.class);
        when(service.filesystem.openForRead(anyString())).thenReturn(new ByteArrayInputStream(new byte[] {1}));

        // Analyzer found a composer and an instrument the archive doesn't know yet
        service.classificationService = mock(ClassificationService.class);
        when(service.classificationService.analyzeImage(any()))
                .thenReturn(new SheetAnalyzerResult(
                        "Some March",
                        null,
                        null,
                        "Known Composer",
                        null,
                        null,
                        null,
                        null,
                        null,
                        new SheetAnalyzerResult.InstrumentationAnalyzerResult(
                                "Flugelhorn", "1", null, null, null, null)));

        musicianRepository = mock(MusicianRepository.class);
        instrumentRepository = mock(InstrumentRepository.class);
        sheetRepository = mock(SheetRepository.class);
        service.musicianRepository = musicianRepository;
        service.instrumentRepository = instrumentRepository;
        service.sheetRepository = sheetRepository;
        when(musicianRepository.findMusicianByName(anyString())).thenReturn(Optional.empty());
        when(instrumentRepository.findCandidates(anyString(), anyDouble(), anyInt()))
                .thenReturn(List.of());
        when(sheetRepository.findByTitle(anyString())).thenReturn(Optional.empty());

        agent = mock(ClassificationAgent.class);
        service.classificationAgent = agent;
    }

    @Test
    void unknownIdsFromAgent_fallBackToPrefilledValues() {
        ClassificationApplyRequest fromAgent = new ClassificationApplyRequest();
        fromAgent.setTitle("Some March");
        fromAgent.setSheetId(UUID.randomUUID());
        fromAgent.setComposerId(UUID.randomUUID());
        fromAgent.setInstrumentId("invented-instrument");
        when(agent.resolve(anyString())).thenReturn(fromAgent);
        when(sheetRepository.findByIdOptional(any())).thenReturn(Optional.empty());
        when(musicianRepository.findByIdOptional(any())).thenReturn(Optional.empty());
        when(instrumentRepository.findByIdOptional(anyString())).thenReturn(Optional.empty());

        ClassificationApplyRequest suggested = service.classify(DOCUMENT_ID).suggested();

        assertNull(suggested.getSheetId(), "unknown sheet → create a new one, as the pre-fill would");
        assertNull(suggested.getComposerId());
        assertEquals("Known Composer", suggested.getComposerName(), "composer name from the pre-fill kept");
        assertNull(suggested.getInstrumentId());
        assertEquals("Flugelhorn", suggested.getInstrumentName(), "instrument name from the pre-fill kept");
    }

    @Test
    void existingIdsFromAgent_areKept() {
        UUID sheetId = UUID.randomUUID();
        UUID composerId = UUID.randomUUID();
        ClassificationApplyRequest fromAgent = new ClassificationApplyRequest();
        fromAgent.setSheetId(sheetId);
        fromAgent.setComposerId(composerId);
        fromAgent.setInstrumentId("flugelhorn");
        when(agent.resolve(anyString())).thenReturn(fromAgent);
        when(sheetRepository.findByIdOptional(sheetId)).thenReturn(Optional.of(new SheetMusicEntity()));
        when(musicianRepository.findByIdOptional(composerId)).thenReturn(Optional.of(new MusicianEntity()));
        when(instrumentRepository.findByIdOptional("flugelhorn")).thenReturn(Optional.of(new InstrumentEntity()));

        ClassificationApplyRequest suggested = service.classify(DOCUMENT_ID).suggested();

        assertEquals(sheetId, suggested.getSheetId());
        assertEquals(composerId, suggested.getComposerId());
        assertEquals("flugelhorn", suggested.getInstrumentId());
    }

    @Test
    void agentReturnsNothing_usesPrefilledSuggestion() {
        when(agent.resolve(anyString())).thenReturn(null);

        SheetClassification result = service.classify(DOCUMENT_ID);

        assertEquals("Some March", result.suggested().getTitle());
        assertEquals("Known Composer", result.suggested().getComposerName());
    }
}
