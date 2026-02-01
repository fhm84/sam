package de.halbmann.sam.business.boundary;

import de.halbmann.sam.api.entity.Attachment;
import de.halbmann.sam.api.entity.AttachmentType;
import de.halbmann.sam.api.entity.DocumentDownload;
import de.halbmann.sam.business.controller.AttachmentMapper;
import de.halbmann.sam.business.entity.*;
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
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;

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

    /**
     * Save incoming document. This will also directly check the file-/content-type of and
     * automatically add related metadata.
     *
     * <p>Future features: - antivirus/malware checks!?
     *
     * @param filename    the (final/display filename)
     * @param inputStream the file content
     */
    public Attachment save(String filename, InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        return save(filename, inputStream, null);
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
    public Attachment save(String filename, InputStream inputStream, AttachmentType attachmentType)
            throws IOException, NoSuchAlgorithmException {
        AttachmentEntity uploaded = upload(
                filename, inputStream, Optional.ofNullable(attachmentType).orElse(AttachmentType.UNSPECIFIED));
        return attachmentMapper.toDto(uploaded);
    }

    public DocumentDownload loadAttachmentByInstrumentation(String instrumentationId, String docId) throws IOException {
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

    public DocumentDownload loadAttachmentBySheet(String sheetId, String docId) throws IOException {
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

    public DocumentDownload loadAttachment(String docIdentifier) throws IOException {
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
    AttachmentEntity upload(String filename, InputStream uploadStream, AttachmentType attachmentType)
            throws IOException, NoSuchAlgorithmException {
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
                doc.setRefCount(1);
                documentRepository.persist(doc);
                return doc;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        if (document.getRefCount() > 0 && document.getId() != null) {
            documentRepository.incrementRefCount(document);
        }

        // Create Attachment linked to SheetMusic
        AttachmentEntity attachment = new AttachmentEntity();
        attachment.setType(attachmentType);
        attachment.setDisplayName(filename);

        // Link to document only if this type is file-based
        if (attachmentType != AttachmentType.EXTERNAL_LINK) {
            attachment.setDocument(document);
        }

        attachmentRepository.persistAndFlush(attachment);
        return attachment;
    }

    DocumentDownload load(UUID documentId) {
        // FIXME: JAX-RS exception!
        DocumentEntity doc = documentRepository
                .findByIdOptional(documentId)
                .orElseThrow(() -> new NotFoundException("Document not found"));

        if (!filesystem.exists(doc.getPath())) {
            throw new IllegalStateException("Physical file missing: " + doc.getPath());
        }

        InputStream stream;
        try {
            stream = filesystem.openForRead(doc.getPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to open document stream", e);
        }

        return new DocumentDownload(
                stream, doc.getId(), doc.getFilename(), doc.getSize(), doc.getMimeType(), doc.getSha256());
    }

    public List<DocumentEntity> listUnlinkedDocuments() {
        return documentRepository.findUnlinked();
    }

    @Transactional
    void deleteIfUnlinked(UUID documentId) {
        DocumentEntity doc = documentRepository.findByIdOptional(documentId).orElseThrow(NotFoundException::new);

        if (doc.getRefCount() > 0) {
            throw new IllegalStateException("Document is still linked");
        }

        try {
            filesystem.delete(doc.getPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete physical file", e);
        }

        documentRepository.delete(doc);
    }
}
