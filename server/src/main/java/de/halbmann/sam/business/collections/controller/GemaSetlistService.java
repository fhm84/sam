package de.halbmann.sam.business.collections.controller;

import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.business.sheets.controller.ExportResult;
import de.halbmann.sam.business.sheets.controller.SheetService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Generates a GEMA setlist by filling the official GEMA Excel upload template
 * with sheet music data from a collection.
 *
 * <p>Template layout (sheet "Setlist Template"):</p>
 * <pre>
 *   Row 19 (idx 18): column headers
 *   Row 20 (idx 19): Vorname/Nachname sub-headers
 *   Row 21+ (idx 20+): data rows
 * </pre>
 *
 * <p>Column mapping (0-based):</p>
 * <ul>
 *   <li>A (0): WERKNUMMER / WERKFASSUNGSNUMMER</li>
 *   <li>B (1): TITEL *</li>
 *   <li>C (2): SATZANGABE / SONSTIGE(R) TITEL</li>
 *   <li>D (3): ANZAHL MUSIKER / SÄNGER (not populated — not available in data model)</li>
 *   <li>E (4): SPIELDAUER (MM:SS)</li>
 *   <li>F (5): INTERPRET / KOMPONIST — Nachname *</li>
 *   <li>G (6): INTERPRET / KOMPONIST — Vorname</li>
 *   <li>H (7): TEXTDICHTER — Nachname (not populated)</li>
 *   <li>I (8): TEXTDICHTER — Vorname (not populated)</li>
 *   <li>J (9): VERLAG</li>
 *   <li>K (10): Art der Musikwiedergabe LIVE/TONTRÄGER (user fills)</li>
 *   <li>L (11): VERÖFFENTLICHTES WERK (user fills)</li>
 *   <li>M (12): GROßES RECHT (user fills)</li>
 *   <li>N (13): POTPOURRI (P) / FRAGMENT (F) (user fills)</li>
 *   <li>O (14): ISWC</li>
 *   <li>P (15): BEARBEITER — Nachname</li>
 *   <li>Q (16): BEARBEITER — Vorname</li>
 * </ul>
 */
@ApplicationScoped
@Transactional
public class GemaSetlistService {

    private static final String TEMPLATE_PATH = "/gema/Excel-Upload-Template_Light_de.xlsx";
    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** 0-based index of the first data row in the template (Excel row 21). */
    private static final int FIRST_DATA_ROW = 20;

    /** Number of pre-formatted empty rows in the template to use as style source. */
    private static final int LAST_COL = 16; // Q

    // Column indices (0-based)
    private static final int COL_WERK_NR = 0;
    private static final int COL_TITEL = 1;
    private static final int COL_SATZ = 2;
    private static final int COL_DAUER = 4;
    private static final int COL_KOMP_NACH = 5;
    private static final int COL_KOMP_VOR = 6;
    private static final int COL_VERLAG = 9;
    private static final int COL_ISWC = 14;
    private static final int COL_BEAR_NACH = 15;
    private static final int COL_BEAR_VOR = 16;

    @Inject
    SheetCollectionService collectionService;

    @Inject
    SheetService sheetService;

    public ExportResult generateGemaSetlist(final String collectionId) {
        SheetCollection collection = collectionService.load(collectionId);
        List<SheetMusic> sheets = collection.getSheets().stream()
                .map(cs -> sheetService.getSheet(cs.getSheetId().toString()))
                .toList();

        try (InputStream templateStream = getClass().getResourceAsStream(TEMPLATE_PATH);
                XSSFWorkbook workbook = new XSSFWorkbook(templateStream)) {

            XSSFSheet sheet = workbook.getSheetAt(0);

            // Capture cell styles from the first pre-formatted empty data row
            Row styleSource = sheet.getRow(FIRST_DATA_ROW);
            CellStyle[] colStyles = extractStyles(styleSource, LAST_COL);

            for (int i = 0; i < sheets.size(); i++) {
                Row row = sheet.createRow(FIRST_DATA_ROW + i);
                if (styleSource != null) {
                    row.setHeight(styleSource.getHeight());
                }
                applyStyles(row, colStyles, LAST_COL);
                fillRow(row, sheets.get(i));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            String filename = sanitizeFilename(collection.getName()) + "-gema-setlist.xlsx";
            return new ExportResult(os -> os.write(out.toByteArray()), filename, CONTENT_TYPE);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate GEMA setlist", e);
        }
    }

    private static void fillRow(final Row row, final SheetMusic s) {
        setString(row, COL_WERK_NR, s.getGemaWorkNumber());
        setString(row, COL_TITEL, s.getTitle());
        setString(row, COL_SATZ, s.getSubtitle());
        setString(row, COL_DAUER, formatDuration(s.getDuration()));
        setMusicianName(row, COL_KOMP_NACH, COL_KOMP_VOR, s.getComposer());
        setString(row, COL_VERLAG, s.getPublisher());
        setString(row, COL_ISWC, s.getIswc());
        setMusicianName(row, COL_BEAR_NACH, COL_BEAR_VOR, s.getArranger());
    }

    private static CellStyle[] extractStyles(final Row styleSource, final int lastCol) {
        CellStyle[] styles = new CellStyle[lastCol + 1];
        if (styleSource == null) return styles;
        for (int c = 0; c <= lastCol; c++) {
            Cell cell = styleSource.getCell(c);
            if (cell != null) {
                styles[c] = cell.getCellStyle();
            }
        }
        return styles;
    }

    private static void applyStyles(final Row row, final CellStyle[] styles, final int lastCol) {
        for (int c = 0; c <= lastCol; c++) {
            Cell cell = row.createCell(c);
            if (styles[c] != null) {
                cell.setCellStyle(styles[c]);
            }
        }
    }

    private static void setString(final Row row, final int col, final String value) {
        if (value == null || value.isBlank()) return;
        Cell cell = row.getCell(col);
        if (cell == null) cell = row.createCell(col);
        cell.setCellValue(value);
    }

    private static void setMusicianName(final Row row, final int colLast, final int colFirst, final Musician musician) {
        if (musician == null || musician.getName() == null) return;
        String[] parts = splitName(musician.getName().trim());
        setString(row, colLast, parts[0]);
        setString(row, colFirst, parts[1]);
    }

    /**
     * Splits a full name into [lastName, firstName] by the last space.
     * "Darius Milhaud" → ["Milhaud", "Darius"]
     * "Bach" → ["Bach", ""]
     * "Johann Sebastian Bach" → ["Bach", "Johann Sebastian"]
     */
    static String[] splitName(final String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[] {"", ""};
        String name = fullName.trim();
        int lastSpace = name.lastIndexOf(' ');
        if (lastSpace > 0) {
            return new String[] {name.substring(lastSpace + 1), name.substring(0, lastSpace)};
        }
        return new String[] {name, ""};
    }

    static String formatDuration(final Duration d) {
        if (d == null) return null;
        long totalSeconds = d.getSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    static String sanitizeFilename(final String name) {
        if (name == null || name.isBlank()) return "collection";
        String s = name.replace("ä", "ae")
                .replace("Ä", "Ae")
                .replace("ö", "oe")
                .replace("Ö", "Oe")
                .replace("ü", "ue")
                .replace("Ü", "Ue")
                .replace("ß", "ss");
        return s.replaceAll("[^a-zA-Z0-9._\\- ]", "_").trim();
    }
}
