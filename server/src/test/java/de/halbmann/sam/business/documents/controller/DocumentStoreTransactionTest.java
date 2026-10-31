package de.halbmann.sam.business.documents.controller;

import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.admin.StorageIntegrityReport;
import de.halbmann.sam.business.documents.boundary.DocumentRepository;
import de.halbmann.sam.business.documents.entity.DocumentEntity;
import de.halbmann.storage.api.FileSystemWrapper;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.narayana.jta.QuarkusTransactionException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The file store follows the transaction: a deleted document's file goes only after the commit, a
 * rolled-back delete keeps it, and a rolled-back upload leaves no file. Plus the integrity check
 * against the real store.
 */
@QuarkusTest
class DocumentStoreTransactionTest {

    @Inject
    DocumentStore documentStore;

    @Inject
    DocumentRepository documentRepository;

    @Inject
    FileSystemWrapper filesystem;

    @Inject
    StorageIntegrityCheck integrityCheck;

    private final List<String> extraFiles = new ArrayList<>();

    private final List<UUID> createdIds = new ArrayList<>();

    @AfterEach
    void cleanUp() throws Exception {
        for (UUID id : createdIds) {
            QuarkusTransaction.requiringNew().run(() -> {
                if (documentRepository.findByIdOptional(id).isPresent()) {
                    documentStore.deleteIfUnlinked(id);
                }
            });
        }
        createdIds.clear();
        for (String path : extraFiles) {
            filesystem.delete(path);
        }
        extraFiles.clear();
    }

    private DocumentEntity upload(String content) {
        DocumentEntity doc = QuarkusTransaction.requiringNew()
                .call(() -> documentStore.save(
                        "tx-test.txt", new ByteArrayInputStream(unique(content).getBytes(StandardCharsets.UTF_8))));
        createdIds.add(doc.getId());
        return doc;
    }

    private static String unique(String content) {
        return content + " " + UUID.randomUUID();
    }

    private boolean rowExists(UUID id) {
        return QuarkusTransaction.requiringNew()
                .call(() -> documentRepository.findByIdOptional(id).isPresent());
    }

    @Test
    void committedDelete_removesRowAndFile() {
        DocumentEntity doc = upload("delete me");

        QuarkusTransaction.requiringNew().run(() -> documentStore.deleteIfUnlinked(doc.getId()));

        assertFalse(rowExists(doc.getId()));
        assertFalse(filesystem.exists(doc.getPath()));
    }

    @Test
    void rolledBackDelete_keepsRowAndFile() {
        DocumentEntity doc = upload("keep me");

        // setRollbackOnly makes run() throw once the rollback is done
        assertThrows(
                QuarkusTransactionException.class,
                () -> QuarkusTransaction.requiringNew().run(() -> {
                    documentStore.deleteIfUnlinked(doc.getId());
                    // Inside the transaction the file must still be there: it goes only after the commit
                    assertTrue(filesystem.exists(doc.getPath()));
                    QuarkusTransaction.setRollbackOnly();
                }));

        assertTrue(rowExists(doc.getId()));
        assertTrue(filesystem.exists(doc.getPath()), "a rolled-back delete must keep the file");
    }

    @Test
    void rolledBackUpload_leavesNoFile() {
        String[] path = new String[1];

        // setRollbackOnly makes run() throw once the rollback is done
        assertThrows(
                QuarkusTransactionException.class,
                () -> QuarkusTransaction.requiringNew().run(() -> {
                    try {
                        path[0] = documentStore
                                .save(
                                        "rollback.txt",
                                        new ByteArrayInputStream(
                                                unique("roll back").getBytes()))
                                .getPath();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                    assertTrue(filesystem.exists(path[0]));
                    QuarkusTransaction.setRollbackOnly();
                }));

        assertFalse(filesystem.exists(path[0]), "the file of a row that never committed must go");
    }

    @Test
    void integrityCheck_reportsDamagedMissingOrphanedAndTempFiles() throws Exception {
        DocumentEntity intact = upload("intact");
        DocumentEntity damaged = upload("damaged");
        DocumentEntity lost = upload("lost");
        filesystem.save(damaged.getPath(), new ByteArrayInputStream("tampered".getBytes(StandardCharsets.UTF_8)));
        filesystem.delete(lost.getPath());
        String orphan = "zz/zz/orphan-" + UUID.randomUUID();
        String temp = DocumentStore.TEMP_DIR + "/" + UUID.randomUUID() + ".tmp";
        String rootTemp = "Klarinette 1 " + UUID.randomUUID() + ".pdf.tmp";
        for (String extra : List.of(orphan, temp, rootTemp)) {
            filesystem.save(extra, new ByteArrayInputStream(new byte[] {1}));
            extraFiles.add(extra);
        }

        StorageIntegrityReport report = integrityCheck.run();

        assertTrue(
                report.mismatches().stream().anyMatch(i -> i.documentId().equals(damaged.getId())),
                "damaged file reported");
        assertTrue(
                report.missingFiles().stream().anyMatch(i -> i.documentId().equals(lost.getId())),
                "missing file reported");
        assertTrue(report.orphanedFiles().contains(orphan), "orphan reported");
        assertTrue(report.leftoverTempFiles().contains(temp), "temp leftover reported");
        assertTrue(report.leftoverTempFiles().contains(rootTemp), "pre-d2d1646 temp file in the root reported");
        assertFalse(report.orphanedFiles().contains(rootTemp));
        assertFalse(
                report.mismatches().stream().anyMatch(i -> i.documentId().equals(intact.getId())),
                "intact file not reported");
        assertFalse(report.orphanedFiles().contains(intact.getPath()));
    }
}
