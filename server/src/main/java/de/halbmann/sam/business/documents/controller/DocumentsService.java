package de.halbmann.sam.business.documents.controller;

import de.halbmann.sam.api.entity.documents.*;
import de.halbmann.sam.business.collections.boundary.SheetCollectionRepository;
import de.halbmann.sam.business.collections.entity.SheetCollectionEntity;
import de.halbmann.sam.business.collections.entity.SheetCollectionItemEntity;
import de.halbmann.sam.business.documents.boundary.AttachmentRepository;
import de.halbmann.sam.business.documents.boundary.DocumentRepository;
import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.documents.entity.DocumentEntity;
import de.halbmann.sam.business.shared.entity.AbstractEntity;
import de.halbmann.sam.business.sheets.boundary.InstrumentationRepository;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.entity.InstrumentationEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.core.entity.PaginatedEntities;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import de.halbmann.sam.core.exception.StorageException;
import de.halbmann.sam.storage.MimeTypeUtils;
import de.halbmann.sam.storage.malware.VirusScanner;
import de.halbmann.sam.storage.upload.UploadContext;
import de.halbmann.sam.storage.upload.UploadPolicy;
import de.halbmann.storage.api.FileSystemWrapper;
import de.halbmann.storage.util.CountingOutputStream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.tika.Tika;

@Slf4j
@ApplicationScoped
@Transactional
public class DocumentsService {

    private static final Tika TIKA = new Tika();

    @Inject
    FileSystemWrapper filesystem;

    @Inject
    VirusScanner virusScanner;

    @Inject
    Instance<UploadPolicy> policies;

    @Inject
    DocumentRepository documentRepository;

    @Inject
    SheetRepository sheetRepository;

    @Inject
    InstrumentationRepository instrumentationRepository;

    @Inject
    AttachmentRepository attachmentRepository;

    @Inject
    SheetCollectionRepository sheetCollectionRepository;

    @Inject
    AttachmentMapper attachmentMapper;

    @Inject
    DocumentMapper documentMapper;

    @Inject
    PdfMetadataEnricher pdfMetadataEnricher;

    /**
     * Save incoming document. This will also directly check the file-/content-type of and
     * automatically add related metadata.
     *
     * <p>Future features: - antivirus/malware checks!?
     *
     * @param filename    the (final/display filename)
     * @param inputStream the file content
     */
    public DocumentEntity save(String filename, InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        return upload(filename, inputStream);
    }

    /**
     * Save incoming document. This will also directly check the file-/content-type of and
     * automatically add related metadata.
     *
     * <p>Future features: - antivirus/malware checks!?
     *
     * @param filename       the (final/display filename)
     * @param inputStream    the file content
     * @param attachmentType specifies the type of the attachment
     */
    public DocumentUpload save(String filename, InputStream inputStream, AttachmentType attachmentType)
            throws IOException, NoSuchAlgorithmException {
        DocumentEntity document = upload(filename, inputStream);
        DocumentDownload documentDownload = documentMapper.toDto(document);

        AttachmentEntity attachment;
        if (attachmentType != null) {
            // Create Attachment linked to SheetMusic
            attachment = new AttachmentEntity();
            attachment.setType(attachmentType);
            attachment.setDisplayName(document.getFilename());

            // Link to document only if this type is file-based
            if (attachmentType != AttachmentType.EXTERNAL_LINK) {
                attachment.setDocument(document);
                documentRepository.incrementRefCount(document);
            }

            attachmentRepository.persistAndFlush(attachment);
        } else {
            attachment = null;
        }

        return new DocumentUpload(documentDownload, attachmentMapper.toDto(attachment));
    }

    public List<Attachment> loadAttachmentsByInstrumentation(String instrumentationId) {
        if (instrumentationId == null) {
            return null;
        }

        InstrumentationEntity instrumentation = instrumentationRepository.findById(UUID.fromString(instrumentationId));
        if (instrumentation != null) {
            return instrumentation.getAttachments().stream()
                    .map(attachmentMapper::toDto)
                    .toList();
        }
        return null;
    }

    public DocumentDownload loadAttachmentByInstrumentation(String instrumentationId, String docId) {
        if (instrumentationId == null) {
            return null;
        }

        InstrumentationEntity instrumentation = instrumentationRepository.findById(UUID.fromString(instrumentationId));
        if (instrumentation != null) {
            Optional<UUID> attachmentId = instrumentation.getAttachments().stream()
                    .map(AbstractEntity::getId)
                    .filter(id -> id.toString().equals(docId))
                    .findFirst();

            DocumentDownload download =
                    loadAttachment(attachmentId.map(String::valueOf).orElse(null));
            if (download == null) {
                return null;
            }
            return pdfMetadataEnricher.enrich(download, instrumentation.getSheet(), instrumentation);
        }
        return null;
    }

    public List<Attachment> loadAttachmentsBySheet(String sheetId) {
        if (sheetId == null) {
            return null;
        }

        SheetMusicEntity sheetMusic = sheetRepository.findById(UUID.fromString(sheetId));
        if (sheetMusic != null) {
            return sheetMusic.getAttachments().stream()
                    .map(attachmentMapper::toDto)
                    .toList();
        }
        return null;
    }

    public DocumentDownload loadAttachmentBySheet(String sheetId, String docId) {
        if (sheetId == null) {
            return null;
        }

        SheetMusicEntity sheetMusic = sheetRepository.findById(UUID.fromString(sheetId));
        if (sheetMusic != null) {
            Optional<UUID> attachmentId = sheetMusic.getAttachments().stream()
                    .map(AbstractEntity::getId)
                    .filter(id -> id.toString().equals(docId))
                    .findFirst();

            DocumentDownload download =
                    loadAttachment(attachmentId.map(String::valueOf).orElse(null));
            if (download == null) {
                return null;
            }
            return pdfMetadataEnricher.enrich(download, sheetMusic, null);
        }
        return null;
    }

    public DocumentDownload loadAttachment(String docIdentifier) {
        if (docIdentifier != null) {
            AttachmentEntity attachment = attachmentRepository.findById(UUID.fromString(docIdentifier));
            if (attachment != null && attachment.getDocument() != null) {
                return load(attachment.getDocument().getId());
            }
        }
        return null;
    }

    /**
     * Uploads a file with deduplication by SHA-256.
     */
    DocumentEntity upload(String filename, InputStream uploadStream) throws IOException, NoSuchAlgorithmException {
        // Prepare streaming digest
        MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
        Path tempPath = Path.of(filename + ".tmp");
        long fileSize;

        try (InputStream scanned = virusScanner.scan(uploadStream);
                DigestInputStream digestIn = new DigestInputStream(scanned, sha256Digest);
                OutputStream rawOut = filesystem.openForWrite(tempPath.toString());
                CountingOutputStream countingOut = new CountingOutputStream(rawOut)) {
            digestIn.transferTo(countingOut);
            fileSize = countingOut.getBytesWritten();
        }

        // Detect MIME type using Tika (from temp file)
        String mimeType = TIKA.detect(filesystem.resolve(tempPath.toString()).toFile());

        // Build UploadContext
        UploadContext context = new UploadContext(uploadStream, filename, fileSize, tempPath, mimeType);

        // Run all policies
        for (UploadPolicy policy : policies) {
            policy.verify(context);
        }

        // Compute SHA-256 hex
        String sha256Hex = HexFormat.of().formatHex(sha256Digest.digest());

        // Deduplication check
        DocumentEntity document = documentRepository.findBySha256(sha256Hex).orElseGet(() -> {
            try {
                String extension = MimeTypeUtils.resolveExtension(mimeType, filename);

                String finalPath = String.format(
                        "%s/%s/%s/%s.%s",
                        sha256Hex.substring(0, 2),
                        sha256Hex.substring(2, 4),
                        sha256Hex.substring(4, 6),
                        sha256Hex,
                        extension);

                filesystem.move(tempPath.toString(), finalPath);

                DocumentEntity doc = new DocumentEntity();
                doc.setFilename(filename);
                doc.setPath(finalPath);
                doc.setSize(fileSize);
                doc.setMimeType(mimeType);
                doc.setSha256(sha256Hex);
                doc.setRefCount(0);
                documentRepository.persist(doc);
                return doc;
            } catch (IOException e) {
                throw new StorageException("Failed to store file", e);
            }
        });

        return document;
    }

    public DocumentDownload load(UUID documentId) {
        DocumentEntity doc = documentRepository
                .findByIdOptional(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document", documentId));

        if (!filesystem.exists(doc.getPath())) {
            throw new StorageException("Physical file missing: " + doc.getPath());
        }

        InputStream stream;
        try {
            stream = filesystem.openForRead(doc.getPath());
        } catch (IOException e) {
            throw new StorageException("Failed to open document stream", e);
        }

        return new DocumentDownload(
                stream, doc.getId(), doc.getFilename(), doc.getSize(), doc.getMimeType(), doc.getSha256());
    }

    public PaginatedEntities<DocumentEntity> listUnlinkedDocuments(int page, int size) {
        return documentRepository.findUnlinked(page, size);
    }

    public void unlinkAttachments(Collection<AttachmentEntity> attachments) {
        for (AttachmentEntity attachment : attachments) {
            DocumentEntity document = attachment.getDocument();
            attachmentRepository.delete(attachment);
            if (document != null) {
                documentRepository.decrementRefCount(document);
                // Intentionally no deleteIfUnlinked: document stays in the uploads pool
            }
        }
    }

    public void deleteAttachment(String attachmentId) {
        AttachmentEntity attachment = attachmentRepository
                .findByIdOptional(UUID.fromString(attachmentId))
                .orElseThrow(() -> new EntityNotFoundException("Attachment", UUID.fromString(attachmentId)));

        instrumentationRepository.removeAttachment(attachment);
        sheetRepository.removeAttachment(attachment);

        DocumentEntity document = attachment.getDocument();
        attachmentRepository.delete(attachment);

        if (document != null) {
            documentRepository.decrementRefCount(document);
            deleteIfUnlinked(document);
        }
    }

    public void linkAttachmentToSheet(String attachmentId, String sheetId) {
        AttachmentEntity attachment = attachmentRepository.findById(UUID.fromString(attachmentId));
        SheetMusicEntity sheet = sheetRepository.findById(UUID.fromString(sheetId));
        if (sheet != null && attachment != null) {
            sheet.getAttachments().add(attachment);
            sheetRepository.persist(sheet);
        }
    }

    public void linkAttachmentToInstrumentation(String attachmentId, String instrumentationId) {
        AttachmentEntity attachment = attachmentRepository.findById(UUID.fromString(attachmentId));
        InstrumentationEntity instrumentation = instrumentationRepository.findById(UUID.fromString(instrumentationId));
        if (instrumentation != null && attachment != null) {
            instrumentation.getAttachments().add(attachment);
            instrumentationRepository.persist(instrumentation);
        }
    }

    void deleteIfUnlinked(DocumentEntity doc) {
        if (doc.getRefCount() > 0) {
            return;
        }

        try {
            filesystem.delete(doc.getPath());
        } catch (IOException e) {
            throw new StorageException("Failed to delete physical file", e);
        }

        documentRepository.delete(doc);
    }

    // ── Batch download helpers ────────────────────────────────────────────────

    /**
     * Load all attachment entities for a sheet, optionally filtered by type.
     */
    public List<AttachmentEntity> loadAttachmentEntitiesBySheet(String sheetId, AttachmentType type) {
        SheetMusicEntity sheet = sheetRepository.findById(UUID.fromString(sheetId));
        if (sheet == null) {
            return List.of();
        }
        return sheet.getAttachments().stream()
                .filter(a -> type == null || type == a.getType())
                .toList();
    }

    /**
     * Load all attachment entities for a single instrumentation, optionally filtered by type.
     */
    public List<AttachmentEntity> loadAttachmentEntitiesByInstrumentation(
            String instrumentationId, AttachmentType type) {
        InstrumentationEntity instr = instrumentationRepository.findById(UUID.fromString(instrumentationId));
        if (instr == null) {
            return List.of();
        }
        return instr.getAttachments().stream()
                .filter(a -> type == null || type == a.getType())
                .toList();
    }

    /**
     * Load attachment entities from a sheet and ALL its instrumentations, optionally filtered by
     * type. Sheet-level attachments come first, then instrumentation attachments sorted by
     * instrument name and partLabel.
     */
    public List<AttachmentEntity> loadAttachmentEntitiesBySheetInstrumentations(String sheetId, AttachmentType type) {
        SheetMusicEntity sheet = sheetRepository.findById(UUID.fromString(sheetId));
        if (sheet == null) {
            return List.of();
        }
        List<AttachmentEntity> result = new ArrayList<>();
        sheet.getAttachments().stream()
                .filter(a -> type == null || type == a.getType())
                .forEach(result::add);
        sheet.getInstrumentations().stream()
                .sorted(Comparator.comparing(
                                (InstrumentationEntity i) -> i.getInstrument().getName())
                        .thenComparing(
                                i -> Optional.ofNullable(i.getPartLabel()).orElse("")))
                .flatMap(i -> i.getAttachments().stream())
                .filter(a -> type == null || type == a.getType())
                .forEach(result::add);
        return result;
    }

    /**
     * Load attachment entities by explicit IDs.
     */
    public List<AttachmentEntity> loadAttachmentEntitiesByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .map(id -> attachmentRepository.findByIdOptional(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Build labeled merge entries for a sheet, optionally including all its instrumentation parts.
     * Sheet-level attachments are labeled with their display name. Instrumentation attachments are
     * labeled with the instrument name (and part label when set), e.g. {@code "Trompete 2"}.
     */
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

    /**
     * Build labeled merge entries for a single instrumentation. All entries share the same label
     * (instrument name + part label), since they all belong to the same instrumentation context.
     */
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

    /**
     * Build labeled merge entries from an explicit list of attachment IDs, labeled by display name.
     */
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

    private String instrumentLabel(InstrumentationEntity instr) {
        String name = Optional.ofNullable(instr.getInstrument().getDisplayName())
                .orElse(instr.getInstrument().getName());
        return instr.getPartLabel() != null ? name + " " + instr.getPartLabel() : name;
    }

    /**
     * Build a streaming ZIP archive from a list of attachments.
     * EXTERNAL_LINK attachments (no physical file) are silently skipped.
     * Duplicate filenames are disambiguated with a counter suffix.
     */
    public StreamWriter buildZip(List<AttachmentEntity> attachments, String zipFilename) {
        return outputStream -> {
            try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
                Set<String> usedNames = new LinkedHashSet<>();
                for (AttachmentEntity att : attachments) {
                    if (att.getDocument() == null) {
                        continue;
                    }
                    String entryName = uniqueZipName(att.getDisplayName(), usedNames);
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

    /**
     * Build a streaming ZIP for a sheet, optionally filtered by type. Includes sheet-level
     * attachments (no prefix) followed by instrumentation attachments prefixed with the instrument
     * label, e.g. {@code "Trumpet 1_score.pdf"}.
     * EXTERNAL_LINK attachments (no physical file) are silently skipped.
     * Duplicate entry names are disambiguated with a counter suffix.
     */
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
                    String entryName = uniqueZipName(rawName, usedNames);
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

    /**
     * Build a streaming ZIP for all instrumentations of every sheet in a collection, filtered by
     * attachment type. Entry names are prefixed with the sheet title and instrument label, e.g.
     * {@code "My Sheet - Trumpet 1_score.pdf"}. Returns {@code null} when the collection has no
     * matching attachments.
     */
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
                    String entryName = uniqueZipName(
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

    /**
     * Build a streaming merged PDF with a PDF outline (bookmarks) from labeled entries.
     * Only entries with mimeType {@code application/pdf} are included; others are silently skipped.
     * Returns {@code null} if no PDF entries are present.
     */
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
     * @param entries     labeled attachment entries; non-PDF entries are skipped
     * @param pathReader  supplies an {@link InputStream} for a given storage path
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

    private String uniqueZipName(String displayName, Set<String> used) {
        String base = displayName != null && !displayName.isBlank() ? displayName : "document";
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

    @Transactional
    public void deleteIfUnlinked(UUID documentId) {
        DocumentEntity doc = documentRepository
                .findByIdOptional(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document", documentId));

        if (doc.getRefCount() > 0) {
            throw new IllegalStateException("Document is still linked");
        }

        try {
            filesystem.delete(doc.getPath());
        } catch (IOException e) {
            throw new StorageException("Failed to delete physical file", e);
        }

        documentRepository.delete(doc);
    }

    /**
     * Link or relink a document/attachment identified by {@code docIdentifier}.
     * <ul>
     *   <li>If the ID resolves to an existing {@link AttachmentEntity} the attachment is moved to
     *       the new target and its type is updated when provided (relink / correct a wrong link).
     *       The physical file is never deleted — {@code refCount} stays unchanged.</li>
     *   <li>Otherwise the ID is treated as a {@link DocumentEntity} and a new attachment is created
     *       and linked to the given target (original behaviour).</li>
     * </ul>
     */
    public Attachment linkDocument(UUID docIdentifier, DocumentLinkRequest documentLink) {
        // Relink path: id belongs to an existing attachment
        Optional<AttachmentEntity> existingAttachment = attachmentRepository.findByIdOptional(docIdentifier);
        if (existingAttachment.isPresent()) {
            AttachmentEntity attachment = existingAttachment.get();

            // Remove from current location (both junction tables, no-op if already unlinked)
            instrumentationRepository.removeAttachment(attachment);
            sheetRepository.removeAttachment(attachment);

            if (documentLink.getAttachmentType() != null) {
                attachment.setType(documentLink.getAttachmentType());
            }

            linkToTarget(attachment, documentLink);
            attachmentRepository.persistAndFlush(attachment);
            return attachmentMapper.toDto(attachment);
        }

        // Create path: id belongs to a document — original behaviour
        DocumentEntity document = documentRepository
                .findByIdOptional(docIdentifier)
                .orElseThrow(() -> new EntityNotFoundException("Document", docIdentifier));

        AttachmentEntity attachment = new AttachmentEntity();
        attachment.setType(Optional.ofNullable(documentLink.getAttachmentType()).orElse(AttachmentType.UNSPECIFIED));
        attachment.setDisplayName(document.getFilename());

        if (documentLink.getAttachmentType() != AttachmentType.EXTERNAL_LINK) {
            attachment.setDocument(document);
            documentRepository.incrementRefCount(document);
        }

        linkToTarget(attachment, documentLink);
        attachmentRepository.persistAndFlush(attachment);
        return attachmentMapper.toDto(attachment);
    }

    private void linkToTarget(AttachmentEntity attachment, DocumentLinkRequest request) {
        if (request.getInstrumentationId() != null) {
            InstrumentationEntity instrumentation = instrumentationRepository
                    .findByIdOptional(request.getInstrumentationId())
                    .orElseThrow(() -> new EntityNotFoundException("Instrumentation", request.getInstrumentationId()));
            instrumentation.getAttachments().add(attachment);
            instrumentationRepository.persist(instrumentation);
        } else if (request.getSheetId() != null) {
            SheetMusicEntity sheet = sheetRepository
                    .findByIdOptional(request.getSheetId())
                    .orElseThrow(() -> new EntityNotFoundException("Sheet", request.getSheetId()));
            sheet.getAttachments().add(attachment);
            sheetRepository.persist(sheet);
        }
        // Neither → attachment stays/becomes unlinked (physical file preserved)
    }
}
