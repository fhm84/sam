package de.halbmann.sam.business.documents.controller;

import de.halbmann.sam.api.entity.admin.StorageIntegrityReport;
import de.halbmann.sam.api.entity.admin.StorageIntegrityReport.StorageIssue;
import de.halbmann.sam.business.documents.boundary.DocumentRepository;
import de.halbmann.sam.core.exception.StorageException;
import de.halbmann.sam.storage.StorageReadinessCheck;
import de.halbmann.storage.api.FileSystemWrapper;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * Compares the file store with the {@code documents} table (document store hardening, task 8):
 * re-hashes every document's file, and lists files no document points to. It finds files damaged by
 * the old temp-file bug and is meant to run before and after the encryption migration. Read-only;
 * deliberately not transactional, so reading the whole archive holds no database transaction.
 */
@Slf4j
@ApplicationScoped
public class StorageIntegrityCheck {

    @Inject
    FileSystemWrapper filesystem;

    @Inject
    DocumentRepository documentRepository;

    private record StoredDocument(UUID id, String filename, String path, String sha256, long size) {}

    public StorageIntegrityReport run() {
        List<StoredDocument> documents = QuarkusTransaction.requiringNew()
                .call(() -> documentRepository.listAll().stream()
                        .map(d ->
                                new StoredDocument(d.getId(), d.getFilename(), d.getPath(), d.getSha256(), d.getSize()))
                        .toList());

        List<StorageIssue> mismatches = new ArrayList<>();
        List<StorageIssue> missing = new ArrayList<>();
        Set<String> knownPaths = new HashSet<>();
        for (StoredDocument doc : documents) {
            knownPaths.add(doc.path());
            check(doc, mismatches, missing);
        }

        List<String> orphaned = new ArrayList<>();
        List<String> leftoverTemp = new ArrayList<>();
        int storedFiles = 0;
        try (Stream<String> files = filesystem.list()) {
            for (String file : (Iterable<String>) files::iterator) {
                storedFiles++;
                if (isLeftoverTempFile(file)) {
                    leftoverTemp.add(file);
                } else if (!knownPaths.contains(file) && !StorageReadinessCheck.PROBE_PATH.equals(file)) {
                    orphaned.add(file);
                }
            }
        } catch (IOException e) {
            throw new StorageException("Failed to list the stored files", e);
        }

        StorageIntegrityReport report =
                new StorageIntegrityReport(documents.size(), storedFiles, mismatches, missing, orphaned, leftoverTemp);
        log.atInfo().log(
                "Storage integrity check: {} documents, {} files; {} mismatched, {} missing, {} orphaned, {} temp leftovers",
                documents.size(),
                storedFiles,
                mismatches.size(),
                missing.size(),
                orphaned.size(),
                leftoverTemp.size());
        return report;
    }

    /**
     * Staging files of older versions: under {@code .upload-tmp/} (since d2d1646), or named
     * {@code <upload file name>.tmp} in the storage root (before that; the bug that corrupted files).
     */
    static boolean isLeftoverTempFile(String file) {
        return file.startsWith(DocumentStore.TEMP_DIR + "/") || (!file.contains("/") && file.endsWith(".tmp"));
    }

    private void check(StoredDocument doc, List<StorageIssue> mismatches, List<StorageIssue> missing) {
        if (!filesystem.exists(doc.path())) {
            missing.add(issue(doc, "no file at the stored path"));
            return;
        }
        MessageDigest digest = sha256();
        long size;
        try (InputStream in = new DigestInputStream(filesystem.openForRead(doc.path()), digest)) {
            size = in.transferTo(OutputStream.nullOutputStream());
        } catch (IOException e) {
            missing.add(issue(doc, "unreadable: " + e.getMessage()));
            return;
        }
        String actual = HexFormat.of().formatHex(digest.digest());
        if (!actual.equals(doc.sha256()) || size != doc.size()) {
            mismatches.add(issue(
                    doc,
                    "expected sha256 " + doc.sha256() + " (" + doc.size() + " bytes), found " + actual + " (" + size
                            + " bytes)"));
        }
    }

    private static StorageIssue issue(StoredDocument doc, String detail) {
        return new StorageIssue(doc.id(), doc.filename(), doc.path(), detail);
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
