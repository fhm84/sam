package de.halbmann.sam.api.entity.documents;

import jakarta.json.bind.annotation.JsonbTransient;
import java.io.InputStream;
import java.util.UUID;

/**
 * Represents a document file ready for download. The {@code stream} field carries the raw bytes
 * and is excluded from JSON serialisation; the remaining fields form the metadata payload.
 */
public record DocumentDownload(
        @JsonbTransient InputStream stream,
        UUID id,
        String filename,
        long size,
        String mimeType,
        String checksumSha256) {}
