package de.halbmann.sam.api.entity.sheets;

import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Request object for batch-downloading a set of attachments as a single file.
 */
@Getter
@Setter
public class BatchDownloadRequest {

    /**
     * IDs of the attachments to include.
     */
    private List<UUID> ids;

    /**
     * Output format: ZIP (default) or MERGED_PDF (PDFs only, falls back to ZIP if non-PDFs are included).
     */
    private DownloadFormat format = DownloadFormat.ZIP;

    /**
     * Optional base name (without extension) to use for the downloaded file.
     * Should already be sanitized by the caller. Falls back to "documents" if absent.
     */
    private String baseName;
}
