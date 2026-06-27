package de.halbmann.sam.business.collections.controller;

import de.halbmann.sam.api.entity.collections.CollectionItemType;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.business.shared.controller.FilenameUtils;
import de.halbmann.sam.business.sheets.controller.ExportResult;
import de.halbmann.sam.business.sheets.controller.SheetService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@ApplicationScoped
@Transactional
public class GemaSetlistService {

    private static final String TEMPLATE_PATH = "/gema/gema-setlist-template.xlsx";
    private static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Inject
    SheetCollectionService collectionService;

    @Inject
    SheetService sheetService;

    public ExportResult generateGemaSetlist(final String collectionId) {
        SheetCollection collection = collectionService.load(collectionId);
        List<SheetMusic> sheets = collection.getItems().stream()
                .filter(i -> i.getType() == CollectionItemType.SHEET)
                .map(cs -> sheetService.getSheet(cs.getSheetId().toString()))
                .toList();

        try (InputStream templateStream = getClass().getResourceAsStream(TEMPLATE_PATH);
                XSSFWorkbook workbook = new XSSFWorkbook(templateStream)) {

            XSSFSheet sheet = workbook.getSheetAt(0);

            Map<String, String> globalVars = buildGlobalVars(collection);
            replaceInSheet(sheet, globalVars);

            int templateRowIdx = findTemplateRow(sheet);
            expandTemplateRow(sheet, templateRowIdx, sheets);

            // Remove all sheets except the filled one
            while (workbook.getNumberOfSheets() > 1) {
                workbook.removeSheetAt(1);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            String filename = FilenameUtils.sanitizeFilename(collection.getName(), "collection") + "-gema-setlist.xlsx";
            return new ExportResult(os -> os.write(out.toByteArray()), filename, CONTENT_TYPE);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate GEMA setlist", e);
        }
    }

    Map<String, String> buildGlobalVars(final SheetCollection collection) {
        Map<String, String> vars = new HashMap<>();
        vars.put("COLLECTION_NAME", collection.getName());
        vars.put("DATE", LocalDate.now().format(DATE_FMT));
        return vars;
    }

    Map<String, String> buildRowVars(final SheetMusic s) {
        Map<String, String> vars = new HashMap<>();
        vars.put("GEMA_WORK_NUMBER", s.getGemaWorkNumber());
        vars.put("TITLE", s.getTitle());
        vars.put("SUBTITLE", s.getSubtitle());
        vars.put("DURATION", formatDuration(s.getDuration()));
        String[] composer = splitName(s.getComposer());
        vars.put("COMPOSER_NAME", composer[0]);
        vars.put("COMPOSER_FIRSTNAME", composer[1]);
        vars.put("PUBLISHER", s.getPublisher());
        vars.put("ISWC", s.getIswc());
        String[] arranger = splitName(s.getArranger());
        vars.put("ARRANGER_NAME", arranger[0]);
        vars.put("ARRANGER_FIRSTNAME", arranger[1]);
        return vars;
    }

    /** Replaces all {{KEY}} placeholders in all cells of the sheet. */
    void replaceInSheet(final XSSFSheet sheet, final Map<String, String> vars) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                replacePlaceholders(cell, vars);
            }
        }
    }

    /** Returns the index of the first row containing any {{...}} placeholder. */
    int findTemplateRow(final XSSFSheet sheet) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING
                        && cell.getStringCellValue().contains("{{")) {
                    return row.getRowNum();
                }
            }
        }
        throw new IllegalStateException("No template row with {{...}} placeholders found in sheet");
    }

    /**
     * Clones the template row for each sheet entry, fills placeholders,
     * then removes the original template row.
     */
    void expandTemplateRow(final XSSFSheet sheet, final int templateRowIdx, final List<SheetMusic> sheets) {
        XSSFRow templateRow = sheet.getRow(templateRowIdx);

        // Snapshot placeholder values before any modification
        Map<Integer, String> templateCellValues = snapshotCellValues(templateRow);

        // Shift rows below the template row down by (N-1) to make room
        int lastRow = sheet.getLastRowNum();
        if (sheets.size() > 1 && templateRowIdx < lastRow) {
            sheet.shiftRows(templateRowIdx + 1, lastRow, sheets.size() - 1);
        }

        for (int i = 0; i < sheets.size(); i++) {
            XSSFRow targetRow;
            if (i == 0) {
                targetRow = templateRow;
            } else {
                targetRow = sheet.createRow(templateRowIdx + i);
                copyRow(templateRow, targetRow, templateCellValues);
            }
            replacePlaceholders(targetRow, buildRowVars(sheets.get(i)));
        }
    }

    Map<Integer, String> snapshotCellValues(final XSSFRow row) {
        Map<Integer, String> snapshot = new HashMap<>();
        for (Cell cell : row) {
            if (cell.getCellType() == CellType.STRING) {
                snapshot.put(cell.getColumnIndex(), cell.getStringCellValue());
            }
        }
        return snapshot;
    }

    void copyRow(final XSSFRow src, final XSSFRow dst, final Map<Integer, String> originalValues) {
        dst.setHeight(src.getHeight());
        for (Cell srcCell : src) {
            XSSFCell dstCell = dst.createCell(srcCell.getColumnIndex(), srcCell.getCellType());
            dstCell.setCellStyle(srcCell.getCellStyle());
            if (srcCell.getCellType() == CellType.STRING) {
                // Use original placeholder values, not the already-replaced ones
                dstCell.setCellValue(originalValues.getOrDefault(srcCell.getColumnIndex(), ""));
            }
        }
        // Copy merged regions that start in the source row
        XSSFSheet sheet = src.getSheet();
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.getFirstRow() == src.getRowNum()) {
                int offset = dst.getRowNum() - src.getRowNum();
                sheet.addMergedRegion(new CellRangeAddress(
                        region.getFirstRow() + offset,
                        region.getLastRow() + offset,
                        region.getFirstColumn(),
                        region.getLastColumn()));
            }
        }
    }

    void replacePlaceholders(final Row row, final Map<String, String> vars) {
        for (Cell cell : row) {
            replacePlaceholders(cell, vars);
        }
    }

    void replacePlaceholders(final Cell cell, final Map<String, String> vars) {
        if (cell.getCellType() != CellType.STRING) {
            return;
        }
        String value = cell.getStringCellValue();
        if (!value.contains("{{")) {
            return;
        }
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            String replacement = entry.getValue() != null ? entry.getValue() : "";
            value = value.replace("{{" + entry.getKey() + "}}", replacement);
        }
        cell.setCellValue(value);
    }

    /**
     * Splits a musician's full name into [lastName, firstName] by the last space.
     * Returns ["", ""] for null/blank input.
     */
    String[] splitName(final Musician musician) {
        if (musician == null || musician.getName() == null) {
            return new String[] {"", ""};
        }
        return splitName(musician.getName().trim());
    }

    String[] splitName(final String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[] {"", ""};
        }
        String name = fullName.trim();
        int lastSpace = name.lastIndexOf(' ');
        if (lastSpace > 0) {
            return new String[] {name.substring(lastSpace + 1), name.substring(0, lastSpace)};
        }
        return new String[] {name, ""};
    }

    String formatDuration(final Duration d) {
        if (d == null) {
            return null;
        }
        long totalSeconds = d.getSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
