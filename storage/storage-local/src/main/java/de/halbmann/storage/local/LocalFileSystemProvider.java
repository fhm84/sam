package de.halbmann.storage.local;

import de.halbmann.storage.api.FileSystemWrapper;
import de.halbmann.storage.spi.FileSystemProvider;
import de.halbmann.storage.spi.StorageLocation;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Path;
import java.nio.file.Paths;

@ApplicationScoped
public class LocalFileSystemProvider implements FileSystemProvider {

    @Override
    public boolean supports(StorageLocation location) {
        return "file".equalsIgnoreCase(location.scheme());
    }

    @Override
    public FileSystemWrapper create(StorageLocation location) {
        Path basePath = Paths.get(location.path()).toAbsolutePath().normalize();

        return new LocalFileSystemWrapper(basePath);
    }
}
