package de.halbmann.sam.cli;

import jakarta.inject.Singleton;

/**
 * Central error output for all commands and importers. Prints a concise message by default; the
 * full stack trace when {@code --stacktrace} was given (the inherited option on
 * {@code SamCliCommand} sets the flag here during parsing).
 */
@Singleton
public class CliErrorReporter {

    private boolean stacktrace;

    public void setStacktrace(final boolean stacktrace) {
        this.stacktrace = stacktrace;
    }

    public boolean isStacktrace() {
        return stacktrace;
    }

    public void printError(final String message, final Exception e) {
        System.err.println(message + (e.getMessage() != null ? ": " + e.getMessage() : ""));
        if (stacktrace) {
            e.printStackTrace(System.err);
        } else {
            System.err.println("  (re-run with --stacktrace for the full stack trace)");
        }
    }
}
