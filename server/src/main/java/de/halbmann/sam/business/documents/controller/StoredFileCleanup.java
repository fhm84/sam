package de.halbmann.sam.business.documents.controller;

import de.halbmann.storage.api.FileSystemWrapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Ties physical file deletes to the outcome of the current transaction, so the file store and the
 * {@code documents} table can't disagree: a deleted row's file goes only after the commit, and a
 * new file whose row never commits goes after the rollback. A file that can't be deleted then is
 * logged and left as an orphan for the integrity check to report.
 */
@Slf4j
@ApplicationScoped
public class StoredFileCleanup {

    @Inject
    FileSystemWrapper filesystem;

    @Inject
    TransactionSynchronizationRegistry transactions;

    /** Deletes the file once the current transaction has committed (at once without one). */
    public void deleteAfterCommit(String path) {
        if (!inTransaction()) {
            deleteQuietly(path);
            return;
        }
        onCompletion(path, Status.STATUS_COMMITTED);
    }

    /** Deletes the file if the current transaction rolls back (nothing without one). */
    public void deleteOnRollback(String path) {
        if (inTransaction()) {
            onCompletion(path, Status.STATUS_ROLLEDBACK);
        }
    }

    /** Deletes a file now; failures are logged, not thrown. */
    public void deleteQuietly(String path) {
        try {
            filesystem.delete(path);
        } catch (Exception e) {
            log.atWarn().setCause(e).log("Could not delete stored file {}; it is left as an orphan", path);
        }
    }

    private boolean inTransaction() {
        return transactions.getTransactionStatus() != Status.STATUS_NO_TRANSACTION;
    }

    private void onCompletion(String path, int expectedStatus) {
        transactions.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
                // nothing to do before the outcome is known
            }

            @Override
            public void afterCompletion(int status) {
                if (status == expectedStatus) {
                    deleteQuietly(path);
                }
            }
        });
    }
}
