package de.halbmann.sam.api.entity.admin;

import java.util.List;
import java.util.UUID;

/**
 * Result of the storage integrity check: every document's file re-hashed and compared with the
 * database, and every stored file matched to a document. Run while nobody uploads: a file whose row
 * hasn't committed yet shows up as orphaned.
 *
 * @param checkedDocuments  documents whose file was looked at
 * @param storedFiles       files found in the storage
 * @param mismatches        files whose SHA-256 or size differs from the database (damaged)
 * @param missingFiles      documents without a file
 * @param orphanedFiles     files no document points to (storage-relative paths)
 * @param leftoverTempFiles upload staging files of older versions ({@code .upload-tmp/…}, or
 *                          {@code <name>.tmp} in the storage root)
 */
public record StorageIntegrityReport(
        int checkedDocuments,
        int storedFiles,
        List<StorageIssue> mismatches,
        List<StorageIssue> missingFiles,
        List<String> orphanedFiles,
        List<String> leftoverTempFiles) {

    /**
     * A document whose file is damaged or missing.
     *
     * @param documentId the document
     * @param filename   its original file name
     * @param path       its storage path
     * @param detail     what is wrong, e.g. the expected and the actual hash
     */
    public record StorageIssue(UUID documentId, String filename, String path, String detail) {}
}
