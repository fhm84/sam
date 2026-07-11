package de.halbmann.sam.cli.entity;

import java.io.File;

public record ImportResult(File file, Status status) {

    public enum Status {
        IMPORTED,
        SKIPPED,
        FAILED
    }

    public static ImportResult imported(final File file) {
        return new ImportResult(file, Status.IMPORTED);
    }

    public static ImportResult skipped(final File file) {
        return new ImportResult(file, Status.SKIPPED);
    }

    public static ImportResult failed(final File file) {
        return new ImportResult(file, Status.FAILED);
    }

    public boolean success() {
        return status != Status.FAILED;
    }
}
