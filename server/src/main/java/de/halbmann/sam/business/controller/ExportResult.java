package de.halbmann.sam.business.controller;

/**
 * Carries the result of a sheet or collection export: a streamable body,
 * a suggested download filename, and the MIME type.
 *
 * <p>The resource layer is responsible for wrapping this in an HTTP {@code Response}.
 */
public record ExportResult(StreamWriter body, String filename, String contentType) {}
