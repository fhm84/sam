package de.halbmann.storage.local;

import de.halbmann.storage.api.FileSystemWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class LocalFileSystemWrapper implements FileSystemWrapper {

    private final Path basePath;

    public LocalFileSystemWrapper(Path basePath) {
        this.basePath = basePath;
    }

    @Override
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
        return Files.newOutputStream(p, StandardOpenOption.CREATE);
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
        Files.move(source, target);
    }

    @Override
    public void delete(String relativePath) throws IOException {
        Files.deleteIfExists(resolve(relativePath));
    }
}
