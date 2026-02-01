package de.halbmann.storage.spi;

import de.halbmann.storage.api.FileSystemWrapper;

public interface FileSystemProvider {

    /**
     * Can this provider handle the given location?
     */
    boolean supports(StorageLocation location);

    /**
     * Create the filesystem wrapper
     */
    FileSystemWrapper create(StorageLocation location);
}
