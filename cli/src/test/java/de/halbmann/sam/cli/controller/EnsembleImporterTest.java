package de.halbmann.sam.cli.controller;

import static de.halbmann.sam.cli.controller.ImporterTestSupport.page;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.halbmann.sam.api.boundary.EnsembleVoicesResource;
import de.halbmann.sam.api.boundary.EnsemblesResource;
import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.boundary.VoiceOptionsResource;
import de.halbmann.sam.api.entity.ensembles.CreateEnsembleVoice;
import de.halbmann.sam.api.entity.ensembles.Ensemble;
import de.halbmann.sam.api.entity.ensembles.EnsembleVoice;
import de.halbmann.sam.api.entity.instruments.CreateVoiceOption;
import de.halbmann.sam.api.entity.instruments.VoiceOption;
import de.halbmann.sam.cli.entity.ImportResult;
import de.halbmann.sam.cli.mapper.DataMapperImpl;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class EnsembleImporterTest {

    private static final UUID ENSEMBLE_ID = UUID.randomUUID();
    private static final UUID TRUMPET_VOICE_ID = UUID.randomUUID();
    private static final UUID NEW_VOICE_ID = UUID.randomUUID();

    @TempDir
    Path dir;

    EnsemblesResource ensembles = mock(EnsemblesResource.class);
    EnsembleVoicesResource voices = mock(EnsembleVoicesResource.class);
    VoiceOptionsResource trumpetOptions = mock(VoiceOptionsResource.class);
    VoiceOptionsResource newVoiceOptions = mock(VoiceOptionsResource.class);
    EnsembleImporter importer;

    @BeforeEach
    void setUp() {
        SamResources client = mock(SamResources.class);
        when(client.ensembles()).thenReturn(ensembles);
        when(ensembles.voices(ENSEMBLE_ID.toString())).thenReturn(voices);
        when(voices.options(TRUMPET_VOICE_ID.toString())).thenReturn(trumpetOptions);
        when(voices.options(NEW_VOICE_ID.toString())).thenReturn(newVoiceOptions);

        // on the target: the ensemble with one voice, which has one of its two options
        Ensemble target = ensemble();
        target.setId(ENSEMBLE_ID);
        when(ensembles.findEnsembles(any())).thenReturn(page(List.of(target)));
        when(voices.listAll()).thenReturn(List.of(voice(TRUMPET_VOICE_ID, "1. Trompete")));
        when(trumpetOptions.listAll()).thenReturn(List.of(option("TROMPETE_BB")));
        when(voices.add(any())).thenReturn(voice(NEW_VOICE_ID, "Flöte"));

        importer = ImporterTestSupport.wire(new EnsembleImporter());
        importer.client = client;
        importer.dataMapper = new DataMapperImpl();
    }

    @Test
    void existingEnsembleGetsMissingVoicesAndOptions() throws Exception {
        ImportResult result = importer.importFile(file(), false, false);

        assertEquals(ImportResult.Status.UPDATED, result.status());
        // the existing voice (label matched ignoring case and spaces) gets only its missing option
        ArgumentCaptor<CreateVoiceOption> trumpet = ArgumentCaptor.forClass(CreateVoiceOption.class);
        verify(trumpetOptions).add(trumpet.capture());
        assertEquals("FLUEGELHORN_BB", trumpet.getValue().getInstrumentId());
        // the missing voice is created with its options
        ArgumentCaptor<CreateEnsembleVoice> created = ArgumentCaptor.forClass(CreateEnsembleVoice.class);
        verify(voices).add(created.capture());
        assertEquals("Flöte", created.getValue().getLabel());
        ArgumentCaptor<CreateVoiceOption> flute = ArgumentCaptor.forClass(CreateVoiceOption.class);
        verify(newVoiceOptions).add(flute.capture());
        assertEquals("FLOETE_C", flute.getValue().getInstrumentId());
        verify(ensembles, never()).add(any());
    }

    @Test
    void dryRunWritesNothing() throws Exception {
        ImportResult result = importer.importFile(file(), true, false);

        assertEquals(ImportResult.Status.UPDATED, result.status(), "what the import would do");
        verify(voices, never()).add(any());
        verify(trumpetOptions, never()).add(any());
    }

    private File file() throws Exception {
        Ensemble ensemble = ensemble();
        EnsembleVoice trumpet = voice(null, "1. trompete ");
        trumpet.setOptions(List.of(option("TROMPETE_BB"), option("FLUEGELHORN_BB")));
        EnsembleVoice flute = voice(null, "Flöte");
        flute.setOptions(List.of(option("FLOETE_C")));
        ensemble.setVoices(List.of(trumpet, flute));
        return ImporterTestSupport.file(dir, "ensemble.json", ensemble);
    }

    private static Ensemble ensemble() {
        Ensemble ensemble = new Ensemble();
        ensemble.setName("Blasorchester");
        return ensemble;
    }

    private static EnsembleVoice voice(final UUID id, final String label) {
        EnsembleVoice voice = new EnsembleVoice();
        voice.setId(id);
        voice.setLabel(label);
        return voice;
    }

    private static VoiceOption option(final String instrumentId) {
        VoiceOption option = new VoiceOption();
        option.setInstrumentId(instrumentId);
        return option;
    }
}
