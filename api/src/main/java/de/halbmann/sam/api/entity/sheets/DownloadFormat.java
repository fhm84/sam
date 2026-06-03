package de.halbmann.sam.api.entity.sheets;

/** Output format for batch document downloads. */
public enum DownloadFormat {
    /** All files bundled into a single ZIP archive. */
    ZIP,
    /** PDF files merged into a single PDF; non-PDF files are silently skipped. */
    MERGED_PDF
}
