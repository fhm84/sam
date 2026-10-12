package de.halbmann.sam.core.entity;

/**
 * JSON error response record.
 *
 * @param status the HTTP status code
 * @param error the HTTP reason phrase, e.g. {@code "Not Found"}
 * @param message a human-readable description of the error; may be {@code null}
 */
public record ErrorResponse(int status, String error, String message) {}
