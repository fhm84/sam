package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.*;
import de.halbmann.sam.business.boundary.AttachmentRepository;
import de.halbmann.sam.business.boundary.DocumentRepository;
import de.halbmann.sam.business.boundary.InstrumentationRepository;
import de.halbmann.sam.business.boundary.SheetRepository;
import de.halbmann.sam.business.entity.*;
import de.halbmann.sam.business.exception.EntityNotFoundException;
import de.halbmann.sam.business.exception.StorageException;
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
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
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
    AttachmentMapper attachmentMapper;

    @Inject
    DocumentMapper documentMapper;

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

            return loadAttachment(attachmentId.map(String::valueOf).orElse(null));
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

            return loadAttachment(attachmentId.map(String::valueOf).orElse(null));
        }
        return null;
    }

    public DocumentDownload loadAttachment(String docIdentifier) {
        if (docIdentifier != null) {
            AttachmentEntity attachment = attachmentRepository.findById(UUID.fromString(docIdentifier));
            if (attachment.getDocument() != null) {
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

        if (document.getRefCount() > 0 && document.getId() != null) {
            documentRepository.incrementRefCount(document);
        }

        return document;
    }

    DocumentDownload load(UUID documentId) {
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

    public List<DocumentEntity> listUnlinkedDocuments() {
        return documentRepository.findUnlinked();
    }

    public void deleteAttachment(String attachmentId) {
        AttachmentEntity attachment = attachmentRepository
                .findByIdOptional(UUID.fromString(attachmentId))
                .orElseThrow(() -> new EntityNotFoundException("Attachment", UUID.fromString(attachmentId)));

        DocumentEntity document = attachment.getDocument();
        attachmentRepository.delete(attachment);

        // FIXME: here we also have to remove the AttachmentEntity from Instrumentation/Sheet, if linked!!!

        if (document != null) {
            documentRepository.decrementRefCount(document);
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

    public void linkDocument(String docIdentifier, DocumentLinkRequest documentLink) {
        DocumentEntity document = documentRepository
                .findByIdOptional(UUID.fromString(docIdentifier))
                .orElseThrow(() -> new EntityNotFoundException("Document", docIdentifier));

        AttachmentEntity attachment = new AttachmentEntity();
        attachment.setType(Optional.ofNullable(documentLink.getAttachmentType()).orElse(AttachmentType.UNSPECIFIED));
        attachment.setDisplayName(document.getFilename());

        // Link to document only if this type is file-based
        if (documentLink.getAttachmentType() != AttachmentType.EXTERNAL_LINK) {
            attachment.setDocument(document);
            documentRepository.incrementRefCount(document);
        }

        if (documentLink.getSheetId() != null) {
            SheetMusicEntity sheet = sheetRepository
                    .findByIdOptional(documentLink.getSheetId())
                    .orElseThrow(() -> new EntityNotFoundException("Sheet", documentLink.getSheetId()));
            sheet.getAttachments().add(attachment);
            sheetRepository.persist(sheet);
        }
        if (documentLink.getInstrumentationId() != null) {
            InstrumentationEntity instrumentation = instrumentationRepository
                    .findByIdOptional(documentLink.getInstrumentationId())
                    .orElseThrow(
                            () -> new EntityNotFoundException("Instrumentation", documentLink.getInstrumentationId()));
            instrumentation.getAttachments().add(attachment);
            instrumentationRepository.persist(instrumentation);
        }
        attachmentRepository.persistAndFlush(attachment);
    }
}
