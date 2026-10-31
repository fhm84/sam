package de.halbmann.sam.core.storage.upload;

/**
 * What an {@link UploadPolicy} gets to decide on, after the upload has been written, scanned and
 * hashed. Deliberately without the content: the upload stream is consumed by then, and a policy that
 * needs bytes would get the file's header instead.
 *
 * @param filename original file name
 * @param size     number of bytes written
 * @param mimeType MIME type detected from the stored content (and the file name)
 */
public record UploadContext(String filename, long size, String mimeType) {}
