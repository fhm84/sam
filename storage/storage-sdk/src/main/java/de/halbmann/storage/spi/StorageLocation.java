package de.halbmann.storage.spi;

import java.net.URI;
import java.nio.file.Path;

public final class StorageLocation {

    private final URI uri;

    private StorageLocation(URI uri) {
        this.uri = uri;
    }

    public static StorageLocation parse(String raw) {
        // No scheme → a local filesystem path, absolute or relative to the working directory.
        // Build the URI from the path, not as "file://" + raw: "file://target/uploads" would make
        // "target" the URI host and leave "/uploads" (the filesystem root) as the path.
        if (!raw.contains("://")) {
            return new StorageLocation(Path.of(raw).toAbsolutePath().normalize().toUri());
        }
        return new StorageLocation(URI.create(raw));
    }

    public URI uri() {
        return uri;
    }

    public String scheme() {
        return uri.getScheme();
    }

    public String path() {
        return uri.getPath();
    }

    public String authority() {
        return uri.getAuthority();
    }
}
