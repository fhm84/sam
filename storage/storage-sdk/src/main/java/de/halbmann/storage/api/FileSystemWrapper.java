package de.halbmann.storage.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

/**
 * Storage for document files, addressed by paths relative to the storage root ({@code /}
 * separators). File content is only reachable through the streams, so every implementation (local
 * disk, S3, an encrypting decorator) behaves the same; there is deliberately no way to get a local
 * {@code Path} for a stored file.
 */
public interface FileSystemWrapper {

    OutputStream openForWrite(String relativePath) throws IOException;

    InputStream openForRead(String relativePath) throws IOException;

    boolean exists(String relativePath);

    default void save(String relativePath, InputStream in) throws IOException {
        try (OutputStream out = openForWrite(relativePath)) {
            in.transferTo(out);
        }
    }

    /** Moves a file, replacing the target if it already exists. */
    void move(String sourceRelativePath, String targetRelativePath) throws IOException;

    void delete(String relativePath) throws IOException;

    /**
     * All files under the storage root, as relative paths with {@code /} separators, in no
     * particular order. The caller must close the stream.
     */
    Stream<String> list() throws IOException;
}
