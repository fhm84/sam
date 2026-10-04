package de.halbmann.storage.local;

import de.halbmann.storage.api.FileSystemWrapper;
import de.halbmann.storage.spi.FileSystemProvider;
import de.halbmann.storage.spi.StorageLocation;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Path;

@ApplicationScoped
public class LocalFileSystemProvider implements FileSystemProvider {

    @Override
    public boolean supports(StorageLocation location) {
        return "file".equalsIgnoreCase(location.scheme());
    }

    @Override
    public FileSystemWrapper create(StorageLocation location) {
        // A host part means a mistyped path ("file://relative/dir" → host "relative"); reject it
        // explicitly — Windows would otherwise silently read it as the UNC share \\relative\dir.
        String host = location.authority();
        if (host != null && !host.isEmpty()) {
            throw new IllegalArgumentException("Storage location " + location.uri()
                    + " has a host part; use a plain path or file:///absolute/path");
        }
        // From the URI, not uri.getPath(): on Windows the path part is "/C:/...", which isn't a valid
        // Path.
        Path basePath = Path.of(location.uri()).toAbsolutePath().normalize();

        return new LocalFileSystemWrapper(basePath);
    }
}
