package de.halbmann.sam.business.documents.controller;

import de.halbmann.sam.api.entity.documents.*;
import de.halbmann.sam.business.documents.boundary.AttachmentRepository;
import de.halbmann.sam.business.documents.boundary.DocumentRepository;
import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.documents.entity.DocumentEntity;
import de.halbmann.sam.core.entity.PaginatedEntities;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import de.halbmann.sam.core.exception.StorageException;
import de.halbmann.sam.core.storage.MimeTypeUtils;
import de.halbmann.sam.core.storage.malware.VirusScanner;
import de.halbmann.sam.core.storage.upload.UploadContext;
import de.halbmann.sam.core.storage.upload.UploadPolicy;
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
import java.util.UUID;
import org.apache.tika.Tika;

@ApplicationScoped
@Transactional
public class DocumentStore {

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
            throw new IllegalStateException("Document is still linked");
        }

        try {
            filesystem.delete(doc.getPath());
        } catch (IOException e) {
            throw new StorageException("Failed to delete physical file", e);
        }

        documentRepository.delete(doc);
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

    DocumentEntity upload(String filename, InputStream uploadStream) throws IOException, NoSuchAlgorithmException {
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

        String mimeType = TIKA.detect(filesystem.resolve(tempPath.toString()).toFile());

        UploadContext context = new UploadContext(uploadStream, filename, fileSize, tempPath, mimeType);

        for (UploadPolicy policy : policies) {
            policy.verify(context);
        }

        String sha256Hex = HexFormat.of().formatHex(sha256Digest.digest());

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
}
