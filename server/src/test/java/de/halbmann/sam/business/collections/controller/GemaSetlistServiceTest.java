package de.halbmann.sam.business.collections.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import de.halbmann.sam.api.entity.collections.CollectionSheet;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.business.sheets.controller.ExportResult;
import de.halbmann.sam.business.sheets.controller.SheetService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GemaSetlistServiceTest {

    @Mock
    SheetCollectionService collectionService;

    @Mock
    SheetService sheetService;

    @InjectMocks
    GemaSetlistService service;

    // ── formatDuration ─────────────────────────────────────────────────────

    @Test
    void formatDurationNull() {
        assertNull(GemaSetlistService.formatDuration(null));
    }

    @Test
    void formatDurationZero() {
        assertEquals("0:00", GemaSetlistService.formatDuration(Duration.ZERO));
    }

    @Test
    void formatDurationSecondsOnly() {
        assertEquals("0:45", GemaSetlistService.formatDuration(Duration.ofSeconds(45)));
    }

    @Test
    void formatDurationMinutesAndSeconds() {
        assertEquals("19:30", GemaSetlistService.formatDuration(Duration.ofSeconds(1170)));
    }

    @Test
    void formatDurationPadsSecondsWithLeadingZero() {
        assertEquals("4:07", GemaSetlistService.formatDuration(Duration.ofSeconds(247)));
    }

    // ── splitName ──────────────────────────────────────────────────────────

    @Test
    void splitNameTwoParts() {
        String[] parts = GemaSetlistService.splitName("Darius Milhaud");
        assertEquals("Milhaud", parts[0]);
        assertEquals("Darius", parts[1]);
    }

    @Test
    void splitNameThreeParts() {
        String[] parts = GemaSetlistService.splitName("Johann Sebastian Bach");
        assertEquals("Bach", parts[0]);
        assertEquals("Johann Sebastian", parts[1]);
    }

    @Test
    void splitNameSingleWord() {
        String[] parts = GemaSetlistService.splitName("Mozart");
        assertEquals("Mozart", parts[0]);
        assertEquals("", parts[1]);
    }

    @Test
    void splitNameNullReturnsEmpties() {
        String[] parts = GemaSetlistService.splitName(null);
        assertEquals("", parts[0]);
        assertEquals("", parts[1]);
    }

    @Test
    void splitNameBlankReturnsEmpties() {
        String[] parts = GemaSetlistService.splitName("   ");
        assertEquals("", parts[0]);
        assertEquals("", parts[1]);
    }

    // ── sanitizeFilename ───────────────────────────────────────────────────

    @Test
    void sanitizeFilenameNullReturnsDefault() {
        assertEquals("collection", GemaSetlistService.sanitizeFilename(null));
    }

    @Test
    void sanitizeFilenameReplacesUmlauts() {
        assertEquals("Fruehlingsstueck", GemaSetlistService.sanitizeFilename("Frühlingsstück"));
    }

    @Test
    void sanitizeFilenameReplacesSpecialChars() {
        assertEquals("Hello_World_", GemaSetlistService.sanitizeFilename("Hello/World!"));
    }

    // ── generateGemaSetlist round-trip ─────────────────────────────────────

    @Test
    void generateGemaSetlistFillsCellsAndReturnsValidXlsx() throws Exception {
        UUID sheetId1 = UUID.randomUUID();
        UUID sheetId2 = UUID.randomUUID();

        SheetCollection collection = collection("Sommerkonzert 2025", sheetId1, sheetId2);
        when(collectionService.load("test-id")).thenReturn(collection);
        when(sheetService.getSheet(sheetId1.toString()))
                .thenReturn(sheet("Le boeuf sur le toit", "Darius Milhaud", null, null, Duration.ofSeconds(1170)));
        when(sheetService.getSheet(sheetId2.toString()))
                .thenReturn(sheet("La mer", "Claude Debussy", "WERK-42", "T-000.000.001-0", Duration.ofMinutes(22)));

        ExportResult result = service.generateGemaSetlist("test-id");

        // Verify metadata
        assertEquals("Sommerkonzert 2025-gema-setlist.xlsx", result.filename());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", result.contentType());

        // Deserialize output and inspect cells
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        result.body().write(out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            XSSFSheet sheet = wb.getSheetAt(0);
            assertEquals("Setlist Template", sheet.getSheetName());

            Row r1 = sheet.getRow(20);
            assertNotNull(r1, "first data row must exist");
            assertEquals("Le boeuf sur le toit", r1.getCell(1).getStringCellValue()); // B: TITEL
            assertEquals("19:30", r1.getCell(4).getStringCellValue()); // E: SPIELDAUER
            assertEquals("Milhaud", r1.getCell(5).getStringCellValue()); // F: KOMP Nachname
            assertEquals("Darius", r1.getCell(6).getStringCellValue()); // G: KOMP Vorname

            Row r2 = sheet.getRow(21);
            assertNotNull(r2, "second data row must exist");
            assertEquals("La mer", r2.getCell(1).getStringCellValue()); // B: TITEL
            assertEquals("WERK-42", r2.getCell(0).getStringCellValue()); // A: WERKNUMMER
            assertEquals("T-000.000.001-0", r2.getCell(14).getStringCellValue()); // O: ISWC
            assertEquals("22:00", r2.getCell(4).getStringCellValue()); // E: SPIELDAUER
            assertEquals("Debussy", r2.getCell(5).getStringCellValue()); // F: KOMP Nachname
            assertEquals("Claude", r2.getCell(6).getStringCellValue()); // G: KOMP Vorname
        }
    }

    @Test
    void generateGemaSetlistNullFieldsProduceNoGarbage() throws Exception {
        UUID sheetId = UUID.randomUUID();
        SheetCollection collection = collection("Konzert", sheetId);
        when(collectionService.load("c1")).thenReturn(collection);
        // sheet with only a title — no composer, no duration, no GEMA numbers
        when(sheetService.getSheet(sheetId.toString())).thenReturn(sheet("Unnamed", null, null, null, null));

        ExportResult result = service.generateGemaSetlist("c1");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        result.body().write(out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Row r1 = wb.getSheetAt(0).getRow(20);
            assertNotNull(r1);
            assertEquals("Unnamed", r1.getCell(1).getStringCellValue()); // B: TITEL
            // cells that had no data must be blank, not null-valued strings
            assertEquals("", r1.getCell(0).getStringCellValue()); // A: WERKNUMMER — empty
            assertEquals("", r1.getCell(4).getStringCellValue()); // E: SPIELDAUER — empty
            assertEquals("", r1.getCell(5).getStringCellValue()); // F: KOMP Nachname — empty
        }
    }

    // ── template sanity ────────────────────────────────────────────────────

    @Test
    void templateIsOnClasspath() {
        assertNotNull(
                GemaSetlistServiceTest.class.getResourceAsStream("/gema/Excel-Upload-Template_Light_de.xlsx"),
                "GEMA Excel template must be present on the classpath at /gema/");
    }

    @Test
    void templateHasSetlistSheet() throws Exception {
        try (InputStream in =
                        GemaSetlistServiceTest.class.getResourceAsStream("/gema/Excel-Upload-Template_Light_de.xlsx");
                XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            assertEquals("Setlist Template", workbook.getSheetAt(0).getSheetName());
        }
    }

    // ── fixtures ───────────────────────────────────────────────────────────

    private static SheetCollection collection(final String name, final UUID... sheetIds) {
        SheetCollection c = new SheetCollection();
        c.setName(name);
        for (UUID id : sheetIds) {
            CollectionSheet cs = new CollectionSheet();
            cs.setSheetId(id);
            cs.setIdentifier("1");
            c.getSheets().add(cs);
        }
        return c;
    }

    private static SheetMusic sheet(
            final String title,
            final String composerName,
            final String gemaWorkNumber,
            final String iswc,
            final Duration duration) {
        SheetMusic s = new SheetMusic();
        s.setTitle(title);
        s.setGemaWorkNumber(gemaWorkNumber);
        s.setIswc(iswc);
        s.setDuration(duration);
        if (composerName != null) {
            Musician m = new Musician();
            m.setName(composerName);
            s.setComposer(m);
        }
        return s;
    }
}
