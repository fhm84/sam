package de.halbmann.sam.cli.controller;

import static de.halbmann.sam.cli.controller.ImporterTestSupport.file;
import static de.halbmann.sam.cli.controller.ImporterTestSupport.page;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.halbmann.sam.api.boundary.InstrumentationsResource;
import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.boundary.SheetsResource;
import de.halbmann.sam.api.entity.instruments.Clef;
import de.halbmann.sam.api.entity.instruments.Instrument;
import de.halbmann.sam.api.entity.sheets.CreateInstrumentation;
import de.halbmann.sam.api.entity.sheets.Instrumentation;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.api.entity.sheets.SheetMusicSearchResult;
import de.halbmann.sam.cli.entity.ImportResult;
import de.halbmann.sam.cli.mapper.DataMapperImpl;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class SheetImporterTest {

    private static final UUID SHEET_ID = UUID.randomUUID();

    @TempDir
    Path dir;

    SheetsResource sheets = mock(SheetsResource.class);
    InstrumentationsResource parts = mock(InstrumentationsResource.class);
    SheetImporter importer;

    @BeforeEach
    void setUp() {
        SamResources client = mock(SamResources.class);
        when(client.sheets()).thenReturn(sheets);
        when(sheets.instrumentations(anyString())).thenReturn(parts);
        when(sheets.findSheets(any())).thenReturn(page(List.of()));
        importer = ImporterTestSupport.wire(new SheetImporter());
        importer.client = client;
        importer.dataMapper = new DataMapperImpl();
    }

    @Test
    void validFixtureIsCreatedInADryRunWithoutWriting() {
        Path path = Paths.get("", "src", "test", "resources", "test-sheet.json");

        ImportResult result = importer.importFile(path.toFile(), true, false);

        assertEquals(ImportResult.Status.CREATED, result.status());
        assertEquals(path.toFile(), result.file());
        verify(sheets, never()).add(any());
    }

    @Test
    void missingFileFails() {
        File missing =
                Paths.get("", "src", "test", "resources", "does-not-exist.json").toFile();

        ImportResult result = importer.importFile(missing, true, false);

        assertFalse(result.success());
    }

    @Test
    void sheetWithoutTitleIsInvalid() {
        // bean validation runs before any lookup; the fixture has no title (@NotBlank)
        Path path = Paths.get("", "src", "test", "resources", "invalid-sheet.json");

        ImportResult result = importer.importFile(path.toFile(), true, false);

        assertEquals(ImportResult.Status.FAILED, result.status());
        verify(sheets, never()).findSheets(any());
    }

    @Test
    void newSheetIsCreatedWithEachPartOnce() throws Exception {
        SheetMusic created = sheet("Garten-Polka", "Verlag A");
        created.setId(SHEET_ID);
        when(sheets.add(any())).thenReturn(created);
        SheetMusic sheet = sheet("Garten-Polka", "Verlag A");
        sheet.setInstrumentations(parts(part("TROMPETE_BB", "1."), part("TROMPETE_BB", "1."), part("FLOETE_C", "1.")));

        ImportResult result = importer.importFile(file(dir, "sheet.json", sheet), false, false);

        assertEquals(ImportResult.Status.CREATED, result.status());
        assertEquals(List.of("TROMPETE_BB", "FLOETE_C"), bulkAdded());
    }

    @Test
    void existingSheetGetsOnlyItsMissingPartsAndTags() throws Exception {
        existing(sheet("Garten-Polka", "Verlag A"), part("TROMPETE_BB", "1."));
        SheetMusic sheet = sheet("Garten-Polka", "Verlag A");
        sheet.setInstrumentations(parts(part("TROMPETE_BB", "1."), part("TROMPETE_BB", "2.")));
        sheet.setTags(Set.of("Fest"));

        ImportResult result = importer.importFile(file(dir, "sheet.json", sheet), false, false);

        assertEquals(ImportResult.Status.UPDATED, result.status());
        assertFalse(result.unappliedDifferences());
        assertEquals(List.of("TROMPETE_BB"), bulkAdded());
        verify(sheets).addTags(SHEET_ID.toString(), Set.of("fest"));
        verify(sheets, never()).add(any());
        verify(sheets, never()).update(anyString(), any());
    }

    @Test
    void dryRunReportsMissingPartsButWritesNothing() throws Exception {
        existing(sheet("Garten-Polka", "Verlag A"));
        SheetMusic sheet = sheet("Garten-Polka", "Verlag A");
        sheet.setInstrumentations(parts(part("TROMPETE_BB", "1.")));

        ImportResult result = importer.importFile(file(dir, "sheet.json", sheet), true, false);

        assertEquals(ImportResult.Status.UPDATED, result.status(), "what the import would do");
        verify(parts, never()).add(anyList());
    }

    @Test
    void differingFieldsAreReportedButOnlyWrittenWithUpdate() throws Exception {
        SheetMusic target = sheet("Garten-Polka", "Verlag A");
        target.setEdition("2. Auflage");
        target.setAdditionalNotes("alt");
        existing(target);
        SheetMusic sheet = sheet("Garten-Polka", "Verlag A");
        sheet.setAdditionalNotes("neu");
        File file = file(dir, "sheet.json", sheet);

        ImportResult reported = importer.importFile(file, false, false);

        assertEquals(ImportResult.Status.UNCHANGED, reported.status());
        assertTrue(reported.unappliedDifferences());
        verify(sheets, never()).update(anyString(), any());

        ImportResult updated = importer.importFile(file, false, true);

        assertEquals(ImportResult.Status.UPDATED, updated.status());
        assertFalse(updated.unappliedDifferences());
        ArgumentCaptor<SheetMusic> written = ArgumentCaptor.forClass(SheetMusic.class);
        verify(sheets).update(eq(SHEET_ID.toString()), written.capture());
        assertEquals("neu", written.getValue().getAdditionalNotes());
        assertEquals("2. Auflage", written.getValue().getEdition(), "a field the file leaves empty is kept");
    }

    @Test
    void identicalSheetIsUnchanged() throws Exception {
        existing(sheet("Garten-Polka", "Verlag A"), part("TROMPETE_BB", "1."));
        SheetMusic sheet = sheet("Garten-Polka", "Verlag A");
        sheet.setInstrumentations(parts(part("TROMPETE_BB", "1.")));

        ImportResult result = importer.importFile(file(dir, "sheet.json", sheet), false, true);

        assertEquals(ImportResult.Status.UNCHANGED, result.status());
        verify(parts, never()).add(anyList());
        verify(sheets, never()).update(anyString(), any());
    }

    @Test
    void sheetIsMatchedByIdBeforeTitle() throws Exception {
        SheetMusic renamed = sheet("Garten-Polka (neu)", "Verlag A");
        renamed.setId(SHEET_ID);
        when(sheets.load(SHEET_ID.toString())).thenReturn(renamed);
        when(parts.listAll()).thenReturn(List.of());
        SheetMusic sheet = sheet("Garten-Polka", "Verlag A");
        sheet.setId(SHEET_ID);

        ImportResult result = importer.importFile(file(dir, "sheet.json", sheet), false, false);

        assertTrue(result.unappliedDifferences(), "same record, the title differs");
        verify(sheets, never()).findSheets(any());
        verify(sheets, never()).add(any());
    }

    /** Puts the sheet on the target, found by title + publisher, with the given parts. */
    private void existing(final SheetMusic target, final Instrumentation... present) {
        target.setId(SHEET_ID);
        SheetMusicSearchResult hit = new SheetMusicSearchResult();
        hit.setId(SHEET_ID);
        hit.setTitle(target.getTitle());
        hit.setPublisher(target.getPublisher());
        when(sheets.findSheets(any())).thenReturn(page(List.of(hit)));
        when(sheets.load(SHEET_ID.toString())).thenReturn(target);
        when(parts.listAll()).thenReturn(List.of(present));
    }

    @SuppressWarnings("unchecked")
    private List<String> bulkAdded() {
        ArgumentCaptor<List<CreateInstrumentation>> added = ArgumentCaptor.forClass(List.class);
        verify(parts).add(added.capture());
        return added.getValue().stream()
                .map(CreateInstrumentation::getInstrumentId)
                .toList();
    }

    private static SheetMusic sheet(final String title, final String publisher) {
        SheetMusic sheet = new SheetMusic();
        sheet.setTitle(title);
        sheet.setPublisher(publisher);
        return sheet;
    }

    private static List<Instrumentation> parts(final Instrumentation... parts) {
        return new ArrayList<>(List.of(parts));
    }

    private static Instrumentation part(final String instrumentId, final String label) {
        Instrument instrument = new Instrument();
        instrument.setId(instrumentId);
        Instrumentation part = new Instrumentation();
        part.setInstrument(instrument);
        part.setPartLabel(label);
        part.setClef(Clef.TREBLE);
        return part;
    }
}
