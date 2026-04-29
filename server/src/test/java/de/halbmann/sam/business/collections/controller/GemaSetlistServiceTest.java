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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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
        assertNull(service.formatDuration(null));
    }

    @Test
    void formatDurationZero() {
        assertEquals("0:00", service.formatDuration(Duration.ZERO));
    }

    @Test
    void formatDurationSecondsOnly() {
        assertEquals("0:45", service.formatDuration(Duration.ofSeconds(45)));
    }

    @Test
    void formatDurationMinutesAndSeconds() {
        assertEquals("19:30", service.formatDuration(Duration.ofSeconds(1170)));
    }

    @Test
    void formatDurationPadsSecondsWithLeadingZero() {
        assertEquals("4:07", service.formatDuration(Duration.ofSeconds(247)));
    }

    // ── splitName ──────────────────────────────────────────────────────────

    @Test
    void splitNameTwoParts() {
        String[] parts = service.splitName("Darius Milhaud");
        assertEquals("Milhaud", parts[0]);
        assertEquals("Darius", parts[1]);
    }

    @Test
    void splitNameThreeParts() {
        String[] parts = service.splitName("Johann Sebastian Bach");
        assertEquals("Bach", parts[0]);
        assertEquals("Johann Sebastian", parts[1]);
    }

    @Test
    void splitNameSingleWord() {
        String[] parts = service.splitName("Mozart");
        assertEquals("Mozart", parts[0]);
        assertEquals("", parts[1]);
    }

    @Test
    void splitNameNullReturnsEmpties() {
        String[] parts = service.splitName((String) null);
        assertEquals("", parts[0]);
        assertEquals("", parts[1]);
    }

    @Test
    void splitNameBlankReturnsEmpties() {
        String[] parts = service.splitName("   ");
        assertEquals("", parts[0]);
        assertEquals("", parts[1]);
    }

    @Test
    void splitNameNullMusicianReturnsEmpties() {
        String[] parts = service.splitName((Musician) null);
        assertEquals("", parts[0]);
        assertEquals("", parts[1]);
    }

    // ── sanitizeFilename ───────────────────────────────────────────────────

    @Test
    void sanitizeFilenameNullReturnsDefault() {
        assertEquals("collection", service.sanitizeFilename(null));
    }

    @Test
    void sanitizeFilenameReplacesUmlauts() {
        assertEquals("Fruehlingsstueck", service.sanitizeFilename("Frühlingsstück"));
    }

    @Test
    void sanitizeFilenameReplacesSpecialChars() {
        assertEquals("Hello_World_", service.sanitizeFilename("Hello/World!"));
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

        assertEquals("Sommerkonzert 2025-gema-setlist.xlsx", result.filename());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", result.contentType());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        result.body().write(out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertEquals(1, wb.getNumberOfSheets(), "output must have exactly one sheet");

            XSSFSheet sheet = wb.getSheetAt(0);
            assertNoUnreplacedPlaceholders(sheet);

            List<String> titles = collectStringValues(sheet);
            assertTrue(titles.contains("Le boeuf sur le toit"), "first title must be present");
            assertTrue(titles.contains("La mer"), "second title must be present");
            assertTrue(titles.contains("Milhaud"), "composer last name must be present");
            assertTrue(titles.contains("Darius"), "composer first name must be present");
            assertTrue(titles.contains("Debussy"), "second composer last name must be present");
            assertTrue(titles.contains("19:30"), "duration of first piece must be present");
            assertTrue(titles.contains("WERK-42"), "GEMA work number must be present");
            assertTrue(titles.contains("T-000.000.001-0"), "ISWC must be present");
        }
    }

    @Test
    void generateGemaSetlistNullFieldsLeaveNoPlaceholders() throws Exception {
        UUID sheetId = UUID.randomUUID();
        SheetCollection collection = collection("Konzert", sheetId);
        when(collectionService.load("c1")).thenReturn(collection);
        when(sheetService.getSheet(sheetId.toString())).thenReturn(sheet("Unnamed", null, null, null, null));

        ExportResult result = service.generateGemaSetlist("c1");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        result.body().write(out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertEquals(1, wb.getNumberOfSheets());
            XSSFSheet sheet = wb.getSheetAt(0);
            assertNoUnreplacedPlaceholders(sheet);

            List<String> values = collectStringValues(sheet);
            assertTrue(values.contains("Unnamed"), "title must be present");
        }
    }

    @Test
    void generateGemaSetlistProducesOneRowPerSheet() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        SheetCollection collection = collection("Test", id1, id2, id3);
        when(collectionService.load("x")).thenReturn(collection);
        when(sheetService.getSheet(id1.toString())).thenReturn(sheet("Title A", null, null, null, null));
        when(sheetService.getSheet(id2.toString())).thenReturn(sheet("Title B", null, null, null, null));
        when(sheetService.getSheet(id3.toString())).thenReturn(sheet("Title C", null, null, null, null));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.generateGemaSetlist("x").body().write(out);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            List<String> values = collectStringValues(wb.getSheetAt(0));
            assertTrue(values.contains("Title A"));
            assertTrue(values.contains("Title B"));
            assertTrue(values.contains("Title C"));
        }
    }

    // ── template sanity ────────────────────────────────────────────────────

    @Test
    void templateIsOnClasspath() {
        assertNotNull(
                GemaSetlistServiceTest.class.getResourceAsStream("/gema/gema-setlist-template.xlsx"),
                "GEMA Excel template must be present on the classpath at /gema/");
    }

    @Test
    void templateHasSetlistSheet() throws Exception {
        try (InputStream in = GemaSetlistServiceTest.class.getResourceAsStream("/gema/gema-setlist-template.xlsx");
                XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            assertEquals("Setlist", workbook.getSheetAt(0).getSheetName());
        }
    }

    @Test
    void templateContainsPlaceholders() throws Exception {
        try (InputStream in = GemaSetlistServiceTest.class.getResourceAsStream("/gema/gema-setlist-template.xlsx");
                XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            List<String> values = collectStringValues(sheet);
            assertTrue(
                    values.stream().anyMatch(v -> v.contains("{{")),
                    "template must contain at least one {{...}} placeholder");
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private static void assertNoUnreplacedPlaceholders(final XSSFSheet sheet) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING) {
                    String val = cell.getStringCellValue();
                    assertFalse(
                            val.contains("{{"), "unreplaced placeholder found: " + val + " at row " + row.getRowNum());
                }
            }
        }
    }

    private static List<String> collectStringValues(final XSSFSheet sheet) {
        List<String> result = new ArrayList<>();
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING) {
                    String v = cell.getStringCellValue();
                    if (!v.isBlank()) result.add(v);
                }
            }
        }
        return result;
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
