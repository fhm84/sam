package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.*;
import de.halbmann.sam.business.entity.AttachmentEntity;
import de.halbmann.storage.api.FileSystemWrapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@ApplicationScoped
@Transactional
public class SheetExportService {

    @Inject
    SheetService sheetService;

    @Inject
    SheetCollectionService collectionService;

    @Inject
    DocumentsService documentsService;

    @Inject
    FileSystemWrapper filesystem;

    @Inject
    Jsonb jsonb;

    public ExportResult exportSheet(String sheetId, ExportFormat format) {
        SheetMusic sheet = sheetService.getSheet(sheetId);
        String safeName = sanitizeFilename(sheet.getTitle());

        if (format == ExportFormat.JSON) {
            String json = jsonb.toJson(sheet);
            return new ExportResult(
                    out -> out.write(json.getBytes(StandardCharsets.UTF_8)), safeName + ".json", "application/json");
        }

        if (format == ExportFormat.CSV) {
            String csv = buildCsv(List.of(sheet));
            return new ExportResult(
                    out -> out.write(csv.getBytes(StandardCharsets.UTF_8)),
                    safeName + ".csv",
                    "text/csv; charset=UTF-8");
        }

        // Pre-load within transaction so the lambda only needs file I/O
        String metadataJson = jsonb.toJson(sheet);
        List<AttachmentEntity> attachments = collectSheetAttachments(sheetId);
        return new ExportResult(buildSheetZip(metadataJson, attachments), safeName + ".zip", "application/zip");
    }

    public ExportResult exportCollection(String collectionId, ExportFormat format) {
        SheetCollection collection = collectionService.load(collectionId);
        String safeName = sanitizeFilename(collection.getName());

        if (format == ExportFormat.JSON) {
            String json = jsonb.toJson(collection);
            return new ExportResult(
                    out -> out.write(json.getBytes(StandardCharsets.UTF_8)), safeName + ".json", "application/json");
        }

        if (format == ExportFormat.CSV) {
            List<SheetMusic> sheets = collection.getSheets().stream()
                    .map(cs -> sheetService.getSheet(cs.getSheetId().toString()))
                    .toList();
            String csv = buildCsv(sheets);
            return new ExportResult(
                    out -> out.write(csv.getBytes(StandardCharsets.UTF_8)),
                    safeName + ".csv",
                    "text/csv; charset=UTF-8");
        }

        return new ExportResult(buildCollectionZip(collection), safeName + ".zip", "application/zip");
    }

    // ── ZIP builders ─────────────────────────────────────────────────────────

    private StreamWriter buildSheetZip(String metadataJson, List<AttachmentEntity> attachments) {
        return outputStream -> {
            try (ZipOutputStream zip = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
                zip.putNextEntry(new ZipEntry("sheet.json"));
                zip.write(metadataJson.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                writeAttachmentsToZip(zip, "documents/", attachments);
            }
        };
    }

    private StreamWriter buildCollectionZip(SheetCollection collection) {
        record SheetExport(String folder, String metadataJson, List<AttachmentEntity> attachments) {}

        // Pre-load all data within the transaction
        String collectionJson = jsonb.toJson(collection);
        List<SheetExport> exports = new ArrayList<>();
        int index = 1;
        for (CollectionSheet cs : collection.getSheets()) {
            SheetMusic sheet = sheetService.getSheet(cs.getSheetId().toString());
            String identifier =
                    cs.getIdentifier() != null && !cs.getIdentifier().isBlank()
                            ? cs.getIdentifier()
                            : String.format("%02d", index);
            String folder = sanitizeFilename(identifier + " - " + sheet.getTitle());
            List<AttachmentEntity> attachments =
                    collectSheetAttachments(cs.getSheetId().toString());
            exports.add(new SheetExport(folder, jsonb.toJson(sheet), attachments));
            index++;
        }

        return outputStream -> {
            try (ZipOutputStream zip = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
                zip.putNextEntry(new ZipEntry("collection.json"));
                zip.write(collectionJson.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();

                Set<String> usedFolders = new LinkedHashSet<>();
                for (SheetExport se : exports) {
                    String folder = uniqueZipName(se.folder(), usedFolders) + "/";
                    zip.putNextEntry(new ZipEntry(folder + "sheet.json"));
                    zip.write(se.metadataJson().getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                    writeAttachmentsToZip(zip, folder + "documents/", se.attachments());
                }
            }
        };
    }

    private void writeAttachmentsToZip(ZipOutputStream zip, String prefix, List<AttachmentEntity> attachments)
            throws IOException {
        Set<String> usedNames = new LinkedHashSet<>();
        for (AttachmentEntity att : attachments) {
            if (att.getDocument() == null) continue;
            String entryName = prefix + uniqueZipName(att.getDisplayName(), usedNames);
            zip.putNextEntry(new ZipEntry(entryName));
            try (InputStream in = filesystem.openForRead(att.getDocument().getPath())) {
                in.transferTo(zip);
            }
            zip.closeEntry();
        }
    }

    // ── CSV builder ──────────────────────────────────────────────────────────

    private static String buildCsv(List<SheetMusic> sheets) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,title,subtitle,composer,arranger,originalBy,genre,style,difficultyLevel,")
                .append("duration,yearOfComposition,publisher,rating,iswc,gemaWorkNumber,tags\n");
        for (SheetMusic s : sheets) {
            sb.append(csvField(s.getId() != null ? s.getId().toString() : "")).append(',');
            sb.append(csvField(s.getTitle())).append(',');
            sb.append(csvField(s.getSubtitle())).append(',');
            sb.append(csvField(s.getComposer() != null ? s.getComposer().getName() : ""))
                    .append(',');
            sb.append(csvField(s.getArranger() != null ? s.getArranger().getName() : ""))
                    .append(',');
            sb.append(csvField(s.getOriginalBy())).append(',');
            sb.append(csvField(s.getGenre() != null ? s.getGenre().name() : "")).append(',');
            sb.append(csvField(s.getStyle() != null ? s.getStyle().name() : "")).append(',');
            sb.append(csvField(
                            s.getDifficultyLevel() != null
                                    ? s.getDifficultyLevel().name()
                                    : ""))
                    .append(',');
            sb.append(csvField(s.getDuration() != null ? s.getDuration().toString() : ""))
                    .append(',');
            sb.append(csvField(
                            s.getYearOfComposition() != null
                                    ? s.getYearOfComposition().toString()
                                    : ""))
                    .append(',');
            sb.append(csvField(s.getPublisher())).append(',');
            sb.append(csvField(s.getRating() != null ? s.getRating().toString() : ""))
                    .append(',');
            sb.append(csvField(s.getIswc())).append(',');
            sb.append(csvField(s.getGemaWorkNumber())).append(',');
            sb.append(csvField(s.getTags() != null ? String.join(";", s.getTags()) : ""))
                    .append('\n');
        }
        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<AttachmentEntity> collectSheetAttachments(String sheetId) {
        List<AttachmentEntity> all = new ArrayList<>();
        all.addAll(documentsService.loadAttachmentEntitiesBySheet(sheetId, null));
        all.addAll(documentsService.loadAttachmentEntitiesBySheetInstrumentations(sheetId, null));
        return all;
    }

    private static String csvField(String value) {
        if (value == null || value.isEmpty()) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "export";
        String s = name.replace("ä", "ae")
                .replace("Ä", "Ae")
                .replace("ö", "oe")
                .replace("Ö", "Oe")
                .replace("ü", "ue")
                .replace("Ü", "Ue")
                .replace("ß", "ss");
        return s.replaceAll("[^a-zA-Z0-9._\\- ]", "_").trim();
    }

    private static String uniqueZipName(String name, Set<String> used) {
        String base = name != null && !name.isBlank() ? name : "document";
        String candidate = base;
        int i = 1;
        while (used.contains(candidate)) {
            int dot = base.lastIndexOf('.');
            candidate = dot > 0 ? base.substring(0, dot) + " (" + i + ")" + base.substring(dot) : base + " (" + i + ")";
            i++;
        }
        used.add(candidate);
        return candidate;
    }
}
