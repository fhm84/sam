package de.halbmann.sam.storage;

import de.halbmann.sam.EnvConsts;
import de.halbmann.storage.api.FileSystemWrapper;
import de.halbmann.storage.spi.FileSystemProvider;
import de.halbmann.storage.spi.StorageLocation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class FileSystemWrapperResolver {

    private final FileSystemWrapper resolved;

    @Inject
    public FileSystemWrapperResolver(
            @ConfigProperty(name = EnvConsts.FILESYSTEM_BASE_PATH) String basePath,
            Instance<FileSystemProvider> providers) {
        StorageLocation location = StorageLocation.parse(basePath);

        this.resolved = providers.stream()
                .filter(p -> p.supports(location))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No FileSystemProvider for " + location.scheme()))
                .create(location);
    }

    @Produces
    public FileSystemWrapper filesystem() {
        return resolved;
    }

}
