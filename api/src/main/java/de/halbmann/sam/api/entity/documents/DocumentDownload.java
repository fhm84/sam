package de.halbmann.sam.api.entity.documents;

import jakarta.json.bind.annotation.JsonbTransient;
import java.io.InputStream;
import java.util.UUID;

public record DocumentDownload(
        @JsonbTransient InputStream stream,
        UUID id,
        String filename,
        long size,
        String mimeType,
        String checksumSha256) {}
