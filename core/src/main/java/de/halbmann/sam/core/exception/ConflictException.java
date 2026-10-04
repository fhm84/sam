package de.halbmann.sam.core.exception;

/**
 * Business exception for a request that conflicts with the current state, e.g. deleting something
 * that is still in use (maps to 409).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
