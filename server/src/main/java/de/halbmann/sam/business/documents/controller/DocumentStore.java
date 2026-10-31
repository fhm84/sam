package de.halbmann.sam.business.documents.controller;

import de.halbmann.sam.api.entity.documents.*;
import de.halbmann.sam.business.documents.boundary.AttachmentRepository;
import de.halbmann.sam.business.documents.boundary.DocumentRepository;
import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.documents.entity.DocumentEntity;
import de.halbmann.sam.core.entity.PaginatedEntities;
import de.halbmann.sam.core.exception.ConflictException;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import de.halbmann.sam.core.exception.StorageException;
import de.halbmann.sam.core.storage.malware.VirusScanner;
import de.halbmann.sam.core.storage.upload.UploadContext;
import de.halbmann.sam.core.storage.upload.UploadPolicy;
import de.halbmann.storage.api.FileSystemWrapper;
import de.halbmann.storage.util.CountingOutputStream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;

@Slf4j
@ApplicationScoped
@Transactional
public class DocumentStore {

    private static final Tika TIKA = new Tika();

    /**
     * Where uploads were staged before they were moved to a content-addressed path. Uploads no
     * longer use it; the integrity check reports files left there by older versions.
     */
    static final String TEMP_DIR = ".upload-tmp";

    @Inject
    FileSystemWrapper filesystem;

    @Inject
    StoredFileCleanup storedFiles;

    @Inject
    VirusScanner virusScanner;

    @Inject
    Instance<UploadPolicy> policies;

    @Inject
    DocumentRepository documentRepository;

    @Inject
    AttachmentRepository attachmentRepository;

    @Inject
    AttachmentMapper attachmentMapper;

    @Inject
    DocumentMapper documentMapper;

    public DocumentEntity save(String filename, InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        return upload(filename, inputStream);
    }

    public DocumentUpload save(String filename, InputStream inputStream, AttachmentType attachmentType)
            throws IOException, NoSuchAlgorithmException {
        DocumentEntity document = upload(filename, inputStream);
        DocumentDownload documentDownload = documentMapper.toDto(document);

        AttachmentEntity attachment;
        if (attachmentType != null) {
            attachment = new AttachmentEntity();
            attachment.setType(attachmentType);
            attachment.setDisplayName(document.getFilename());

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

    public DocumentDownload loadAttachment(String docIdentifier) {
        if (docIdentifier != null) {
            AttachmentEntity attachment = attachmentRepository.findById(UUID.fromString(docIdentifier));
            if (attachment != null && attachment.getDocument() != null) {
                return load(attachment.getDocument().getId());
            }
        }
        return null;
    }

    public PaginatedEntities<DocumentEntity> listUnlinkedDocuments(int page, int size) {
        return documentRepository.findUnlinked(page, size);
    }

    public void deleteIfUnlinked(UUID documentId) {
        DocumentEntity doc = documentRepository
                .findByIdOptional(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Document", documentId));

        if (doc.getRefCount() > 0) {
            throw new ConflictException("Document is still linked to " + doc.getRefCount() + " attachment(s)");
        }

        delete(doc);
    }

    void deleteIfUnlinked(DocumentEntity doc) {
        if (doc.getRefCount() > 0) {
            return;
        }

        delete(doc);
    }

    /** The row goes now, the file only once that has committed: a rollback keeps both. */
    private void delete(DocumentEntity doc) {
        documentRepository.delete(doc);
        storedFiles.deleteAfterCommit(doc.getPath());
    }

    DocumentEntity upload(String filename, InputStream uploadStream) throws IOException, NoSuchAlgorithmException {
        MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
        // Written straight to its final, random path: no temp file, no move, and the path never
        // changes after the write (an encrypting store binds the ciphertext to it, ADR-0011)
        String path = newStoragePath();
        boolean kept = false;

        try {
            long fileSize;
            try (InputStream scanned = virusScanner.scan(uploadStream);
                    DigestInputStream digestIn = new DigestInputStream(scanned, sha256Digest);
                    OutputStream rawOut = filesystem.openForWrite(path);
                    CountingOutputStream countingOut = new CountingOutputStream(rawOut)) {
                digestIn.transferTo(countingOut);
                fileSize = countingOut.getBytesWritten();
            }

            // Read back through the wrapper (not a local file) so this also works for S3
            String mimeType;
            try (InputStream in = new BufferedInputStream(filesystem.openForRead(path))) {
                mimeType = TIKA.detect(in, filename);
            }

            UploadContext context = new UploadContext(filename, fileSize, mimeType);
            for (UploadPolicy policy : policies) {
                policy.verify(context);
            }

            String sha256Hex = HexFormat.of().formatHex(sha256Digest.digest());

            // Deduplication stays on the hash in the database; the new copy is dropped below
            Optional<DocumentEntity> existing = documentRepository.findBySha256(sha256Hex);
            if (existing.isPresent()) {
                return existing.get();
            }

            DocumentEntity doc = new DocumentEntity();
            doc.setFilename(filename);
            doc.setPath(path);
            doc.setSize(fileSize);
            doc.setMimeType(mimeType);
            doc.setSha256(sha256Hex);
            doc.setRefCount(0);
            documentRepository.persist(doc);
            kept = true;
            // The row may still roll back with the rest of the request; then the file goes too
            storedFiles.deleteOnRollback(path);
            return doc;
        } finally {
            if (!kept) {
                storedFiles.deleteQuietly(path);
            }
        }
    }

    /**
     * A new random storage path, {@code <uuid[0:2]>/<uuid[2:4]>/<uuid>}: it says nothing about the
     * content (unlike the hash paths of older rows, which stay as they are) and spreads files over
     * directories.
     */
    static String newStoragePath() {
        String id = UUID.randomUUID().toString();
        return id.substring(0, 2) + "/" + id.substring(2, 4) + "/" + id;
    }
}
