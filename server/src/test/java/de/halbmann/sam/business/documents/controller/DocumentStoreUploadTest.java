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
import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;
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
 * Uploads against a real {@link LocalFileSystemWrapper}: each file goes straight to its own random
 * path, and whatever doesn't end up as a document (a duplicate, a rejected or failed upload) leaves
 * nothing behind. Regression context: the old temp file was named after the user's file name and
 * reused, so a smaller upload with the same name was stored with the previous file's tail.
 */
class DocumentStoreUploadTest {

    private static final String RANDOM_PATH = "[0-9a-f]{2}/[0-9a-f]{2}/[0-9a-f]{8}-[0-9a-f-]{27}";

    @TempDir
    Path storageRoot;

    DocumentStore documentStore;

    DocumentRepository documentRepository;

    VirusScanner virusScanner;

    UploadPolicy policy;

    @BeforeEach
    void setUp() throws IOException {
        documentRepository = mock(DocumentRepository.class);
        when(documentRepository.findBySha256(anyString())).thenReturn(Optional.empty());

        virusScanner = mock(VirusScanner.class);
        when(virusScanner.scan(any())).thenAnswer(inv -> inv.<InputStream>getArgument(0));

        policy = mock(UploadPolicy.class);
        @SuppressWarnings("unchecked")
        Instance<UploadPolicy> policies = mock(Instance.class);
        when(policies.iterator()).thenAnswer(inv -> List.of(policy).iterator());

        LocalFileSystemWrapper filesystem = new LocalFileSystemWrapper(storageRoot);
        StoredFileCleanup storedFiles = new StoredFileCleanup();
        storedFiles.filesystem = filesystem;
        storedFiles.transactions = mock(TransactionSynchronizationRegistry.class);
        when(storedFiles.transactions.getTransactionStatus()).thenReturn(Status.STATUS_NO_TRANSACTION);

        documentStore = new DocumentStore();
        documentStore.filesystem = filesystem;
        documentStore.storedFiles = storedFiles;
        documentStore.virusScanner = virusScanner;
        documentStore.policies = policies;
        documentStore.documentRepository = documentRepository;
    }

    private DocumentEntity upload(String filename, String content) throws Exception {
        return documentStore.save(filename, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private List<String> storedFiles() throws IOException {
        try (Stream<String> files = documentStore.filesystem.list()) {
            return files.toList();
        }
    }

    private DocumentEntity existingDocument() {
        DocumentEntity existing = new DocumentEntity();
        existing.setId(UUID.randomUUID());
        return existing;
    }

    @Test
    void newUpload_storesExactBytes_atARandomPath() throws Exception {
        DocumentEntity doc = upload("scan0001.txt", "hello");

        assertTrue(doc.getPath().matches(RANDOM_PATH), doc.getPath());
        assertFalse(doc.getPath().contains(doc.getSha256()), "the path must not reveal the content");
        assertEquals("hello", Files.readString(storageRoot.resolve(doc.getPath())));
        assertEquals(5, doc.getSize());
        assertEquals(List.of(doc.getPath()), storedFiles(), "no temp file, no other copy");
    }

    @Test
    void duplicateUpload_deletesTheNewCopy() throws Exception {
        DocumentEntity existing = existingDocument();
        when(documentRepository.findBySha256(anyString())).thenReturn(Optional.of(existing));

        assertSame(existing, upload("scan0001.txt", "already stored"));
        assertEquals(List.of(), storedFiles());
    }

    @Test
    void rejectedUpload_deletesTheNewCopy() throws Exception {
        doThrow(new IOException("rejected")).when(policy).verify(any());

        assertThrows(IOException.class, () -> upload("scan0001.txt", "not allowed"));
        assertEquals(List.of(), storedFiles());
    }

    @Test
    void failedScan_deletesWhatWasWritten() throws Exception {
        when(virusScanner.scan(any())).thenThrow(new IOException("scanner down"));

        assertThrows(IOException.class, () -> upload("scan0001.txt", "content"));
        assertEquals(List.of(), storedFiles());
    }

    @Test
    void sameFilenameAfterLongerDuplicate_storesExactBytes() throws Exception {
        // 1st: a long file that turns out to be a duplicate
        when(documentRepository.findBySha256(anyString())).thenReturn(Optional.of(existingDocument()));
        upload("scan0001.txt", "OLD-CONTENT-THAT-IS-MUCH-LONGER");

        // 2nd: a different, shorter file with the same name
        when(documentRepository.findBySha256(anyString())).thenReturn(Optional.empty());
        DocumentEntity doc = upload("scan0001.txt", "NEW");

        assertEquals("NEW", Files.readString(storageRoot.resolve(doc.getPath())));
        assertEquals(List.of(doc.getPath()), storedFiles());
    }

    @Test
    void twoUploads_getDifferentPaths() throws Exception {
        assertNotEquals(upload("a.txt", "one").getPath(), upload("a.txt", "two").getPath());
    }

    @Test
    void mimeType_isDetectedFromTheStoredContent() throws Exception {
        DocumentEntity doc = upload("part.pdf", "%PDF-1.4\n% part\n%%EOF\n");

        assertEquals("application/pdf", doc.getMimeType());
    }

    @Test
    void localWrapper_openForWrite_truncatesExistingFile() throws Exception {
        LocalFileSystemWrapper fs = new LocalFileSystemWrapper(storageRoot);
        fs.save("f.txt", new ByteArrayInputStream("LONGER".getBytes(StandardCharsets.UTF_8)));
        fs.save("f.txt", new ByteArrayInputStream("AB".getBytes(StandardCharsets.UTF_8)));

        assertEquals("AB", Files.readString(storageRoot.resolve("f.txt")));
    }

    @Test
    void localWrapper_list_returnsRelativeSlashPaths() throws Exception {
        LocalFileSystemWrapper fs = new LocalFileSystemWrapper(storageRoot);
        fs.save("ab/cd/file", new ByteArrayInputStream(new byte[] {1}));
        fs.save("top.txt", new ByteArrayInputStream(new byte[] {2}));

        try (Stream<String> files = fs.list()) {
            assertEquals(List.of("ab/cd/file", "top.txt"), files.sorted().toList());
        }
    }
}
