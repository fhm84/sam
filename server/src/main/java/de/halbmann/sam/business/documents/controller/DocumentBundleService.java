package de.halbmann.sam.business.documents.controller;

import de.halbmann.sam.api.entity.documents.AttachmentType;
import de.halbmann.sam.business.collections.boundary.SheetCollectionRepository;
import de.halbmann.sam.business.collections.entity.SheetCollectionEntity;
import de.halbmann.sam.business.collections.entity.SheetCollectionItemEntity;
import de.halbmann.sam.business.documents.boundary.AttachmentRepository;
import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.shared.controller.FilenameUtils;
import de.halbmann.sam.business.sheets.boundary.InstrumentationRepository;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.entity.InstrumentationEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.core.exception.StorageException;
import de.halbmann.storage.api.FileSystemWrapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

@ApplicationScoped
@Transactional
public class DocumentBundleService {

    @Inject
    FileSystemWrapper filesystem;

    @Inject
    SheetRepository sheetRepository;

    @Inject
    InstrumentationRepository instrumentationRepository;

    @Inject
    SheetCollectionRepository sheetCollectionRepository;

    @Inject
    AttachmentRepository attachmentRepository;

    public List<MergedPdfEntry> buildMergeEntriesForSheet(
            String sheetId, AttachmentType type, boolean includeInstrumentations) {
        SheetMusicEntity sheet = sheetRepository.findById(UUID.fromString(sheetId));
        if (sheet == null) {
            return List.of();
        }

        List<MergedPdfEntry> entries = new ArrayList<>();

        sheet.getAttachments().stream()
                .filter(a -> type == null || type == a.getType())
                .map(a -> new MergedPdfEntry(a, a.getDisplayName()))
                .forEach(entries::add);

        if (includeInstrumentations) {
            sheet.getInstrumentations().stream()
                    .sorted(Comparator.comparing((InstrumentationEntity i) ->
                                    i.getInstrument().getName())
                            .thenComparing(
                                    i -> Optional.ofNullable(i.getPartLabel()).orElse("")))
                    .forEach(instr -> {
                        String label = instrumentLabel(instr);
                        instr.getAttachments().stream()
                                .filter(a -> type == null || type == a.getType())
                                .map(a -> new MergedPdfEntry(a, label))
                                .forEach(entries::add);
                    });
        }

        return Collections.unmodifiableList(entries);
    }

    public List<MergedPdfEntry> buildMergeEntriesForInstrumentation(String instrumentationId, AttachmentType type) {
        InstrumentationEntity instr = instrumentationRepository.findById(UUID.fromString(instrumentationId));
        if (instr == null) {
            return List.of();
        }
        String label = instrumentLabel(instr);
        return instr.getAttachments().stream()
                .filter(a -> type == null || type == a.getType())
                .map(a -> new MergedPdfEntry(a, label))
                .toList();
    }

    public List<MergedPdfEntry> buildMergeEntriesById(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .map(id -> attachmentRepository.findByIdOptional(id).orElse(null))
                .filter(Objects::nonNull)
                .map(a -> new MergedPdfEntry(a, a.getDisplayName()))
                .toList();
    }

    public StreamWriter buildZip(List<AttachmentEntity> attachments, String zipFilename) {
        return outputStream -> {
            try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
                Set<String> usedNames = new LinkedHashSet<>();
                for (AttachmentEntity att : attachments) {
                    if (att.getDocument() == null) {
                        continue;
                    }
                    String entryName = FilenameUtils.uniqueZipName(att.getDisplayName(), usedNames);
                    zip.putNextEntry(new ZipEntry(entryName));
                    try (InputStream in =
                            filesystem.openForRead(att.getDocument().getPath())) {
                        in.transferTo(zip);
                    }
                    zip.closeEntry();
                }
            }
        };
    }

    public StreamWriter buildZipForSheetInstrumentations(String sheetId, AttachmentType type) {
        SheetMusicEntity sheet = sheetRepository.findById(UUID.fromString(sheetId));
        if (sheet == null) {
            return outputStream -> {};
        }
        record LabeledAttachment(String label, AttachmentEntity attachment) {}
        List<LabeledAttachment> sheetEntries = sheet.getAttachments().stream()
                .filter(a -> type == null || type == a.getType())
                .filter(a -> a.getDocument() != null)
                .map(a -> new LabeledAttachment(null, a))
                .toList();
        List<LabeledAttachment> instrEntries = sheet.getInstrumentations().stream()
                .sorted(Comparator.comparing(
                                (InstrumentationEntity i) -> i.getInstrument().getName())
                        .thenComparing(
                                i -> Optional.ofNullable(i.getPartLabel()).orElse("")))
                .flatMap(instr -> {
                    String label = instrumentLabel(instr);
                    return instr.getAttachments().stream()
                            .filter(a -> type == null || type == a.getType())
                            .filter(a -> a.getDocument() != null)
                            .map(a -> new LabeledAttachment(label, a));
                })
                .toList();
        List<LabeledAttachment> entries =
                Stream.concat(sheetEntries.stream(), instrEntries.stream()).toList();
        return outputStream -> {
            try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
                Set<String> usedNames = new LinkedHashSet<>();
                for (LabeledAttachment entry : entries) {
                    String rawName = entry.label() != null
                            ? entry.label() + "_" + entry.attachment().getDisplayName()
                            : entry.attachment().getDisplayName();
                    String entryName = FilenameUtils.uniqueZipName(rawName, usedNames);
                    zip.putNextEntry(new ZipEntry(entryName));
                    try (InputStream in = filesystem.openForRead(
                            entry.attachment().getDocument().getPath())) {
                        in.transferTo(zip);
                    }
                    zip.closeEntry();
                }
            }
        };
    }

    public StreamWriter buildZipForCollectionInstrumentations(String collectionId, AttachmentType type) {
        SheetCollectionEntity collection = sheetCollectionRepository.findById(UUID.fromString(collectionId));
        if (collection == null) {
            return null;
        }
        record LabeledAttachment(String label, AttachmentEntity attachment) {}
        List<LabeledAttachment> entries = collection.getItems().stream()
                .filter(SheetCollectionItemEntity.class::isInstance)
                .map(SheetCollectionItemEntity.class::cast)
                .map(SheetCollectionItemEntity::getSheet)
                .flatMap(sheet -> sheet.getInstrumentations().stream()
                        .sorted(Comparator.comparing((InstrumentationEntity i) ->
                                        i.getInstrument().getName())
                                .thenComparing(i ->
                                        Optional.ofNullable(i.getPartLabel()).orElse("")))
                        .flatMap(instr -> instr.getAttachments().stream()
                                .filter(a -> type == null || type == a.getType())
                                .filter(a -> a.getDocument() != null)
                                .map(a -> new LabeledAttachment(sheet.getTitle() + " - " + instrumentLabel(instr), a))))
                .toList();
        if (entries.isEmpty()) {
            return null;
        }
        return outputStream -> {
            try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
                Set<String> usedNames = new LinkedHashSet<>();
                for (LabeledAttachment entry : entries) {
                    String entryName = FilenameUtils.uniqueZipName(
                            entry.label() + "_" + entry.attachment().getDisplayName(), usedNames);
                    zip.putNextEntry(new ZipEntry(entryName));
                    try (InputStream in = filesystem.openForRead(
                            entry.attachment().getDocument().getPath())) {
                        in.transferTo(zip);
                    }
                    zip.closeEntry();
                }
            }
        };
    }

    public StreamWriter buildMergedPdf(List<MergedPdfEntry> entries) {
        return doMerge(entries, path -> {
            try {
                return filesystem.openForRead(path);
            } catch (IOException e) {
                throw new StorageException("Failed to open PDF for merge: " + path, e);
            }
        });
    }

    /**
     * Core merge logic, separated from filesystem access so it can be tested with in-memory content.
     *
     * @param entries    labeled attachment entries; non-PDF entries are skipped
     * @param pathReader supplies an {@link InputStream} for a given storage path
     * @return a {@link StreamWriter} that writes the merged PDF, or {@code null} if no PDFs found
     */
    static StreamWriter doMerge(List<MergedPdfEntry> entries, Function<String, InputStream> pathReader) {
        List<MergedPdfEntry> pdfEntries = entries.stream()
                .filter(e -> e.attachment().getDocument() != null)
                .filter(e ->
                        "application/pdf".equals(e.attachment().getDocument().getMimeType()))
                .toList();
        if (pdfEntries.isEmpty()) {
            return null;
        }

        return outputStream -> {
            PDFMergerUtility merger = new PDFMergerUtility();
            List<PDDocument> sources = new ArrayList<>();
            try (PDDocument target = new PDDocument()) {
                PDDocumentOutline outline = new PDDocumentOutline();
                target.getDocumentCatalog().setDocumentOutline(outline);

                int pageOffset = 0;
                for (MergedPdfEntry entry : pdfEntries) {
                    try (InputStream in =
                            pathReader.apply(entry.attachment().getDocument().getPath())) {
                        PDDocument src = Loader.loadPDF(in.readAllBytes());
                        sources.add(src);
                        int srcPageCount = src.getNumberOfPages();
                        merger.appendDocument(target, src);

                        PDPageFitDestination dest = new PDPageFitDestination();
                        dest.setPage(target.getPage(pageOffset));

                        PDOutlineItem item = new PDOutlineItem();
                        item.setTitle(entry.bookmarkLabel() != null ? entry.bookmarkLabel() : "");
                        item.setDestination(dest);
                        outline.addLast(item);

                        pageOffset += srcPageCount;
                    }
                }
                target.save(outputStream);
            } finally {
                for (PDDocument src : sources) {
                    try {
                        src.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        };
    }

    private String instrumentLabel(InstrumentationEntity instr) {
        String name = Optional.ofNullable(instr.getInstrument().getDisplayName())
                .orElse(instr.getInstrument().getName());
        return instr.getPartLabel() != null ? name + " " + instr.getPartLabel() : name;
    }
}
