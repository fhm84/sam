package de.halbmann.sam.api.entity;

import java.io.InputStream;
import java.util.UUID;

public record DocumentDownload(
        InputStream stream,
        UUID id,
        String filename,
        long size,
        String mimeType,
        String checksumSha256
) {
}