package de.halbmann.storage.local;

import de.halbmann.storage.api.FileSystemWrapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

public class LocalFileSystemWrapper implements FileSystemWrapper {

    private final Path basePath;

    public LocalFileSystemWrapper(Path basePath) {
        this.basePath = basePath;
    }

    /**
     * The local file behind a relative path, checked against path traversal. Not part of {@link
     * FileSystemWrapper}: application code reads and writes through the streams only.
     */
    public Path resolve(String relativePath) {
        Path resolved = basePath.resolve(relativePath).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new SecurityException("Path traversal detected");
        }
        return resolved;
    }

    @Override
    public OutputStream openForWrite(String relativePath) throws IOException {
        Path p = resolve(relativePath);
        Files.createDirectories(p.getParent());
        // TRUNCATE_EXISTING: without it, overwriting a longer existing file keeps its trailing bytes
        return Files.newOutputStream(
                p, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    @Override
    public InputStream openForRead(String relativePath) throws IOException {
        return Files.newInputStream(resolve(relativePath));
    }

    @Override
    public boolean exists(String relativePath) {
        return Files.exists(resolve(relativePath));
    }

    @Override
    public void move(String sourceRelativePath, String targetRelativePath) throws IOException {
        Path source = resolve(sourceRelativePath);
        Path target = resolve(targetRelativePath);
        Files.createDirectories(target.getParent());
        // Replace, as S3's copyObject does: targets are content-addressed, so a file already at the
        // target (e.g. left behind without a DB row) has identical content.
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public void delete(String relativePath) throws IOException {
        Files.deleteIfExists(resolve(relativePath));
    }

    @Override
    public Stream<String> list() throws IOException {
        if (!Files.isDirectory(basePath)) {
            return Stream.empty();
        }
        return Files.walk(basePath)
                .filter(Files::isRegularFile)
                .map(p -> basePath.relativize(p).toString().replace(File.separatorChar, '/'));
    }
}
