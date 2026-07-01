package de.halbmann.sam.core.storage.upload;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Context object to carry all relevant metadata about an upload.
 *
 * @param inputStream stream for Tika/Virus scan
 * @param filename    original filename
 * @param size        number of bytes written (after streaming)
 * @param tempPath    optional temp storage path
 * @param mimeType    optional MIME type detected
 */
public record UploadContext(InputStream inputStream, String filename, long size, Path tempPath, String mimeType) {}
