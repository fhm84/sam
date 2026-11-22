package de.halbmann.sam.cli.entity;

import java.io.File;

/**
 * Outcome of importing one file (ADR-0019).
 *
 * @param unappliedDifferences the record exists and some of its fields differ from the file, but
 *     they were left alone (no {@code --update})
 */
public record ImportResult(File file, Status status, boolean unappliedDifferences) {

    public enum Status {
        /** The record did not exist and was created. */
        CREATED,
        /** The record existed; missing child records were added or fields updated. */
        UPDATED,
        /** The record existed and nothing was written. */
        UNCHANGED,
        FAILED
    }

    public static ImportResult created(final File file) {
        return new ImportResult(file, Status.CREATED, false);
    }

    public static ImportResult failed(final File file) {
        return new ImportResult(file, Status.FAILED, false);
    }

    public boolean success() {
        return status != Status.FAILED;
    }
}
