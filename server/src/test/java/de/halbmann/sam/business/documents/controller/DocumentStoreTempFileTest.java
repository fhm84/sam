package de.halbmann.sam.business.documents.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.halbmann.sam.business.documents.boundary.DocumentRepository;
import de.halbmann.sam.business.documents.entity.DocumentEntity;
import de.halbmann.sam.core.storage.malware.VirusScanner;
import de.halbmann.sam.core.storage.upload.UploadPolicy;
import de.halbmann.storage.local.LocalFileSystemWrapper;
import jakarta.enterprise.inject.Instance;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests for the upload temp file, run against a real {@link LocalFileSystemWrapper}.
 * The temp file used to be named after the user's filename, was opened without truncation and was
 * left behind on dedupe or policy rejection — so a later, smaller upload with the same filename
 * was stored with the previous file's trailing bytes appended.
 */
class DocumentStoreTempFileTest {

    @TempDir
    Path storageRoot;

    DocumentStore documentStore;

    DocumentRepository documentRepository;

    UploadPolicy policy;

    @BeforeEach
    void setUp() throws IOException {
        documentRepository = mock(DocumentRepository.class);
        when(documentRepository.findBySha256(anyString())).thenReturn(Optional.empty());

        VirusScanner virusScanner = mock(VirusScanner.class);
        when(virusScanner.scan(any())).thenAnswer(inv -> inv.<InputStream>getArgument(0));

        policy = mock(UploadPolicy.class);
        @SuppressWarnings("unchecked")
        Instance<UploadPolicy> policies = mock(Instance.class);
        when(policies.iterator()).thenAnswer(inv -> List.of(policy).iterator());

        documentStore = new DocumentStore();
        documentStore.filesystem = new LocalFileSystemWrapper(storageRoot);
        documentStore.virusScanner = virusScanner;
        documentStore.policies = policies;
        documentStore.documentRepository = documentRepository;
    }

    private DocumentEntity upload(String filename, String content) throws Exception {
        return documentStore.save(filename, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private List<Path> tempFiles() throws IOException {
        Path tempDir = storageRoot.resolve(DocumentStore.TEMP_DIR);
        if (!Files.exists(tempDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(tempDir)) {
            return files.toList();
        }
    }

    @Test
    void newUpload_storesExactBytes_andLeavesNoTempFile() throws Exception {
        DocumentEntity doc = upload("scan0001.txt", "hello");

        assertEquals("hello", Files.readString(storageRoot.resolve(doc.getPath())));
        assertEquals(5, doc.getSize());
        assertTrue(tempFiles().isEmpty());
    }

    @Test
    void duplicateUpload_deletesTempFile() throws Exception {
        DocumentEntity existing = new DocumentEntity();
        existing.setId(UUID.randomUUID());
        when(documentRepository.findBySha256(anyString())).thenReturn(Optional.of(existing));

        assertSame(existing, upload("scan0001.txt", "already stored"));
        assertTrue(tempFiles().isEmpty());
    }

    @Test
    void rejectedUpload_deletesTempFile() throws Exception {
        doThrow(new IOException("rejected")).when(policy).verify(any());

        assertThrows(IOException.class, () -> upload("scan0001.txt", "not allowed"));
        assertTrue(tempFiles().isEmpty());
    }

    @Test
    void sameFilenameAfterLongerDuplicate_storesExactBytes() throws Exception {
        // 1st: a long file that turns out to be a duplicate (formerly left "scan0001.txt.tmp" behind)
        DocumentEntity existing = new DocumentEntity();
        existing.setId(UUID.randomUUID());
        when(documentRepository.findBySha256(anyString())).thenReturn(Optional.of(existing));
        upload("scan0001.txt", "OLD-CONTENT-THAT-IS-MUCH-LONGER");

        // 2nd: a different, shorter file with the same name
        when(documentRepository.findBySha256(anyString())).thenReturn(Optional.empty());
        DocumentEntity doc = upload("scan0001.txt", "NEW");

        assertEquals("NEW", Files.readString(storageRoot.resolve(doc.getPath())));
        assertTrue(tempFiles().isEmpty());
    }

    @Test
    void localWrapper_openForWrite_truncatesExistingFile() throws Exception {
        LocalFileSystemWrapper fs = new LocalFileSystemWrapper(storageRoot);
        fs.save("f.txt", new ByteArrayInputStream("LONGER".getBytes(StandardCharsets.UTF_8)));
        fs.save("f.txt", new ByteArrayInputStream("AB".getBytes(StandardCharsets.UTF_8)));

        assertEquals("AB", Files.readString(storageRoot.resolve("f.txt")));
    }
}
