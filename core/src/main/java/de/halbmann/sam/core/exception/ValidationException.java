package de.halbmann.sam.core.exception;

/**
 * Business exception for invalid request parameters (maps to 400).
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
