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
import de.halbmann.sam.core.exception.ConflictException;
import de.halbmann.sam.core.exception.StorageException;
import de.halbmann.sam.core.exception.ValidationException;
import de.halbmann.storage.api.FileSystemWrapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

@Slf4j
@ApplicationScoped
@Transactional
public class DocumentBundleService {

    /** Far more than any real selection (a set of parts is a few dozen files); keeps a request bounded. */
    static final int MAX_BATCH_DOCUMENTS = 200;

    /**
     * Merged PDFs built at the same time. Each one copies its parts and the result through temp files;
     * five large ones at once kept the CPU and disk so busy that the server stopped answering.
     */
    private static final Semaphore MERGE_SLOTS = new Semaphore(2, true);

    /** How long a merge waits for a slot before the request is turned away. */
    private static final Duration MERGE_WAIT = Duration.ofMinutes(2);

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

    @Inject
    DownloadNames downloadNames;

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

    /** The attachments with the given IDs, each once. At most {@link #MAX_BATCH_DOCUMENTS}. */
    public List<MergedPdfEntry> buildMergeEntriesById(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<UUID> distinctIds = ids.stream().distinct().toList();
        if (distinctIds.size() > MAX_BATCH_DOCUMENTS) {
            throw new ValidationException("A download can hold at most " + MAX_BATCH_DOCUMENTS + " documents");
        }
        return distinctIds.stream()
                .map(id -> attachmentRepository.findByIdOptional(id).orElse(null))
                .filter(Objects::nonNull)
                .map(a -> new MergedPdfEntry(a, a.getDisplayName()))
                .toList();
    }

    /** A ZIP of the given attachments, each named by {@link DownloadNames}. */
    public StreamWriter buildZip(List<AttachmentEntity> attachments, Locale locale) {
        List<NamedFile> files = attachments.stream()
                .filter(att -> att.getDocument() != null)
                .map(att -> new NamedFile(
                        downloadNames.forAttachment(att, locale),
                        att.getDocument().getPath()))
                .toList();
        return zipOf(files);
    }

    /** A ZIP entry: its name and the stored file it holds. */
    private record NamedFile(String name, String path) {}

    private StreamWriter zipOf(List<NamedFile> files) {
        return outputStream -> {
            try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
                Set<String> usedNames = new LinkedHashSet<>();
                for (NamedFile file : files) {
                    zip.putNextEntry(new ZipEntry(FilenameUtils.uniqueZipName(file.name(), usedNames)));
                    try (InputStream in = filesystem.openForRead(file.path())) {
                        in.transferTo(zip);
                    }
                    zip.closeEntry();
                }
            }
        };
    }

    public StreamWriter buildZipForSheetInstrumentations(String sheetId, AttachmentType type, Locale locale) {
        SheetMusicEntity sheet = sheetRepository.findById(UUID.fromString(sheetId));
        if (sheet == null) {
            return outputStream -> {};
        }
        List<NamedFile> sheetFiles = sheet.getAttachments().stream()
                .filter(a -> type == null || type == a.getType())
                .filter(a -> a.getDocument() != null)
                .map(a -> new NamedFile(
                        downloadNames.forSheet(sheet, a, locale),
                        a.getDocument().getPath()))
                .toList();
        List<NamedFile> instrumentationFiles = sheet.getInstrumentations().stream()
                .sorted(INSTRUMENTATION_ORDER)
                .flatMap(instr -> instr.getAttachments().stream()
                        .filter(a -> type == null || type == a.getType())
                        .filter(a -> a.getDocument() != null)
                        .map(a -> new NamedFile(
                                downloadNames.forInstrumentation(sheet, instr, a, locale),
                                a.getDocument().getPath())))
                .toList();
        return zipOf(Stream.concat(sheetFiles.stream(), instrumentationFiles.stream())
                .toList());
    }

    public StreamWriter buildZipForCollectionInstrumentations(String collectionId, AttachmentType type, Locale locale) {
        SheetCollectionEntity collection = sheetCollectionRepository.findById(UUID.fromString(collectionId));
        if (collection == null) {
            return null;
        }
        List<NamedFile> files = collection.getItems().stream()
                .filter(SheetCollectionItemEntity.class::isInstance)
                .map(SheetCollectionItemEntity.class::cast)
                .map(SheetCollectionItemEntity::getSheet)
                .flatMap(sheet -> sheet.getInstrumentations().stream()
                        .sorted(INSTRUMENTATION_ORDER)
                        .flatMap(instr -> instr.getAttachments().stream()
                                .filter(a -> type == null || type == a.getType())
                                .filter(a -> a.getDocument() != null)
                                .map(a -> new NamedFile(
                                        downloadNames.forInstrumentation(sheet, instr, a, locale),
                                        a.getDocument().getPath()))))
                .toList();
        if (files.isEmpty()) {
            return null;
        }
        return zipOf(files);
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
        return doMerge(entries, pathReader, MERGE_SLOTS, MERGE_WAIT);
    }

    /** As {@link #doMerge(List, Function)}, waiting at most {@code wait} for one of {@code slots}. */
    static StreamWriter doMerge(
            List<MergedPdfEntry> entries, Function<String, InputStream> pathReader, Semaphore slots, Duration wait) {
        List<MergedPdfEntry> pdfEntries = entries.stream()
                .filter(e -> e.attachment().getDocument() != null)
                .filter(e ->
                        "application/pdf".equals(e.attachment().getDocument().getMimeType()))
                .toList();
        if (pdfEntries.isEmpty()) {
            return null;
        }

        return outputStream -> {
            if (!acquire(slots, wait)) {
                throw new ConflictException("Too many merged PDFs are being built right now; try again in a minute");
            }
            try {
                writeMerged(pdfEntries, pathReader, outputStream);
            } finally {
                slots.release();
            }
        };
    }

    private static boolean acquire(Semaphore slots, Duration wait) throws InterruptedIOException {
        try {
            return slots.tryAcquire(wait.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while waiting to merge PDFs");
        }
    }

    private static void writeMerged(
            List<MergedPdfEntry> pdfEntries, Function<String, InputStream> pathReader, OutputStream outputStream)
            throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        // On disk, not in the heap: a set of scanned parts easily adds up to more than the JVM has,
        // and running out of memory stops the whole server (-XX:+ExitOnOutOfMemoryError)
        try (PDDocument target = new PDDocument(IOUtils.createTempFileOnlyStreamCache())) {
            PDDocumentOutline outline = new PDDocumentOutline();
            target.getDocumentCatalog().setDocumentOutline(outline);

            int pageOffset = 0;
            for (MergedPdfEntry entry : pdfEntries) {
                int srcPageCount = appendCopy(target, merger, pathReader, entry);

                PDPageFitDestination dest = new PDPageFitDestination();
                dest.setPage(target.getPage(pageOffset));

                PDOutlineItem item = new PDOutlineItem();
                item.setTitle(entry.bookmarkLabel() != null ? entry.bookmarkLabel() : "");
                item.setDestination(dest);
                outline.addLast(item);

                pageOffset += srcPageCount;
            }
            target.save(outputStream);
        }
    }

    /**
     * Copies an entry's pages into {@code target} and returns how many there were. The source is closed
     * right away: appending copies its content, and each open source keeps a read cache in the heap,
     * which added up to hundreds of MB per merge.
     */
    private static int appendCopy(
            PDDocument target, PDFMergerUtility merger, Function<String, InputStream> pathReader, MergedPdfEntry entry)
            throws IOException {
        Path copy = Files.createTempFile("sam-merge-", ".pdf");
        try {
            try (InputStream in =
                    pathReader.apply(entry.attachment().getDocument().getPath())) {
                Files.copy(in, copy, StandardCopyOption.REPLACE_EXISTING);
            }
            try (PDDocument src = Loader.loadPDF(copy.toFile(), IOUtils.createTempFileOnlyStreamCache())) {
                int pages = src.getNumberOfPages();
                merger.appendDocument(target, src);
                return pages;
            }
        } finally {
            try {
                Files.deleteIfExists(copy);
            } catch (IOException e) {
                log.atWarn().setCause(e).log(() -> "Could not delete the temporary file " + copy);
            }
        }
    }

    private String instrumentLabel(InstrumentationEntity instr) {
        return DownloadNames.instrumentLabel(instr);
    }

    private static final Comparator<InstrumentationEntity> INSTRUMENTATION_ORDER = Comparator.comparing(
                    (InstrumentationEntity i) -> i.getInstrument().getName())
            .thenComparing(i -> Optional.ofNullable(i.getPartLabel()).orElse(""));
}
