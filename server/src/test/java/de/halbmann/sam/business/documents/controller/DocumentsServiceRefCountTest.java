package de.halbmann.sam.business.documents.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.halbmann.sam.api.entity.documents.*;
import de.halbmann.sam.business.collections.boundary.SheetCollectionRepository;
import de.halbmann.sam.business.documents.boundary.AttachmentRepository;
import de.halbmann.sam.business.documents.boundary.DocumentRepository;
import de.halbmann.sam.business.documents.entity.AttachmentEntity;
import de.halbmann.sam.business.documents.entity.DocumentEntity;
import de.halbmann.sam.business.sheets.boundary.InstrumentationRepository;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.storage.malware.VirusScanner;
import de.halbmann.sam.storage.upload.UploadPolicy;
import de.halbmann.storage.api.FileSystemWrapper;
import jakarta.enterprise.inject.Instance;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentsServiceRefCountTest {

    @TempDir
    Path tempDir;

    @Mock
    FileSystemWrapper filesystem;

    @Mock
    VirusScanner virusScanner;

    @Mock
    Instance<UploadPolicy> policies;

    @Mock
    DocumentRepository documentRepository;

    @Mock
    SheetRepository sheetRepository;

    @Mock
    InstrumentationRepository instrumentationRepository;

    @Mock
    AttachmentRepository attachmentRepository;

    @Mock
    SheetCollectionRepository sheetCollectionRepository;

    @Mock
    AttachmentMapper attachmentMapper;

    @Mock
    DocumentMapper documentMapper;

    @Mock
    PdfMetadataEnricher pdfMetadataEnricher;

    @InjectMocks
    DocumentsService service;

    private static final byte[] CONTENT = "hello refcount test".getBytes();

    /** Simulates a document that is already stored and linked (refCount = 2). */
    private DocumentEntity existingDoc() {
        DocumentEntity doc = new DocumentEntity();
        doc.setId(UUID.randomUUID());
        doc.setRefCount(2);
        doc.setFilename("existing.txt");
        doc.setPath("ab/cd/ef/abc123.txt");
        doc.setSize(CONTENT.length);
        doc.setMimeType("text/plain");
        doc.setSha256("abc123");
        return doc;
    }

    /**
     * Stubs the filesystem and virus-scanner so upload() can run without real I/O.
     * Tika MIME detection reads from the resolved path, so we point it at a real temp file.
     */
    private void stubUploadInfrastructure() throws IOException {
        Path tempFile = tempDir.resolve("upload.tmp");
        when(virusScanner.scan(any())).thenAnswer(inv -> inv.<InputStream>getArgument(0));
        when(filesystem.openForWrite(anyString())).thenReturn(Files.newOutputStream(tempFile));
        when(filesystem.resolve(anyString())).thenReturn(tempFile);
        when(policies.iterator()).thenReturn(Collections.emptyIterator());
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    /**
     * Uploading the same file a second time (SHA-256 match) must NOT increment refCount.
     * Reference counting is solely the responsibility of the attachment-creation callers.
     */
    @Test
    void upload_duplicate_neverIncrementsRefCount() throws IOException, NoSuchAlgorithmException {
        stubUploadInfrastructure();
        DocumentEntity existing = existingDoc();
        when(documentRepository.findBySha256(anyString())).thenReturn(Optional.of(existing));

        DocumentEntity result = service.save("test.txt", new ByteArrayInputStream(CONTENT));

        verify(documentRepository, never()).incrementRefCount(any());
        assertEquals(2, existing.getRefCount());
        assertSame(existing, result);
    }

    /**
     * save(filename, stream, type) on a duplicate must increment refCount exactly once —
     * via the attachment-creation path, never inside upload().
     * Before the fix this was incremented twice: once in upload() and once in save().
     */
    @Test
    void save_withType_duplicate_incrementsRefCountExactlyOnce() throws IOException, NoSuchAlgorithmException {
        stubUploadInfrastructure();
        DocumentEntity existing = existingDoc();
        when(documentRepository.findBySha256(anyString())).thenReturn(Optional.of(existing));
        when(documentMapper.toDto(existing))
                .thenReturn(new DocumentDownload(
                        InputStream.nullInputStream(),
                        existing.getId(),
                        "existing.txt",
                        CONTENT.length,
                        "text/plain",
                        "abc123"));
        when(attachmentMapper.toDto(any(AttachmentEntity.class))).thenReturn(new Attachment());

        service.save("test.txt", new ByteArrayInputStream(CONTENT), AttachmentType.PART);

        verify(documentRepository, times(1)).incrementRefCount(existing);
    }
}
