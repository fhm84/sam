package de.halbmann.sam.core.entity;

/**
 * JSON error response record.
 * @param status
 * @param error
 * @param message
 */
public record ErrorResponse(int status, String error, String message) {}
