package de.halbmann.sam.core.exception;

/**
 * Business exception for IO/storage failures (maps to 500).
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
