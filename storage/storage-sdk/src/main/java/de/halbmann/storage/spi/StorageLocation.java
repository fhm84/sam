package de.halbmann.storage.spi;

import java.net.URI;

public final class StorageLocation {

    private final URI uri;

    private StorageLocation(URI uri) {
        this.uri = uri;
    }

    public static StorageLocation parse(String raw) {
        // No scheme → assume local filesystem
        if (!raw.contains("://")) {
            return new StorageLocation(URI.create("file://" + raw.replace("\\", "/")));
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