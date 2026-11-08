package de.halbmann.sam.api.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import de.halbmann.sam.api.entity.documents.DocumentDownload;
import de.halbmann.sam.business.documents.boundary.AttachmentRepository;
import de.halbmann.sam.business.documents.controller.DocumentStore;
import de.halbmann.sam.business.eventlog.controller.EventLogService;
import de.halbmann.sam.security.CurrentUserService;
import de.halbmann.sam.security.Roles;
import jakarta.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The stored file is opened before the ETag check: a 304 sends nothing, so it must close it itself
 * (else every revalidation leaks a file handle, with S3 a pooled connection).
 */
@ExtendWith(MockitoExtension.class)
class DocumentsResourceImplStreamTest {

    @Mock
    DocumentStore documentStore;

    @Mock
    AttachmentRepository attachmentRepository;

    @Mock
    CurrentUserService currentUserService;

    @Mock
    EventLogService eventLogService;

    @InjectMocks
    DocumentsResourceImpl resource;

    private final UUID documentId = UUID.randomUUID();
    private final TrackingStream stream = new TrackingStream();

    static final class TrackingStream extends ByteArrayInputStream {
        boolean closed;

        TrackingStream() {
            super("%PDF-1.7".getBytes());
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @BeforeEach
    void bareDocumentForALibrarian() {
        when(attachmentRepository.findByIdOptional(documentId)).thenReturn(Optional.empty());
        when(currentUserService.hasRole(Roles.MUSIC_LIBRARIAN)).thenReturn(true);
        when(documentStore.load(documentId))
                .thenReturn(new DocumentDownload(stream, documentId, "scan.pdf", 8, "application/pdf", "abc"));
    }

    @Test
    void notModified_closesTheUnsentFile() {
        try (Response response = resource.load(documentId.toString(), "\"sha256:abc\"")) {
            assertEquals(304, response.getStatus());
        }
        assertTrue(stream.closed);
    }

    @Test
    void download_leavesTheFileToTheStreamingBody() {
        try (Response response = resource.load(documentId.toString(), null)) {
            assertEquals(200, response.getStatus());
        }
        assertFalse(stream.closed);
    }
}
