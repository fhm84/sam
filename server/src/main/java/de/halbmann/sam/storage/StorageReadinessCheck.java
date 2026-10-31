package de.halbmann.sam.storage;

import de.halbmann.storage.api.FileSystemWrapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Readiness: SAM can write to its document storage. Writes and deletes a small probe file, so a
 * volume that is missing, read-only, owned by another user or full shows up as DOWN instead of as
 * failing uploads. Goes through {@link FileSystemWrapper}, so it works for local and S3 storage.
 */
@Readiness
@ApplicationScoped
public class StorageReadinessCheck implements HealthCheck {

    /** Written and deleted on every check; the integrity check doesn't count it as an orphan. */
    public static final String PROBE_PATH = ".health-probe";

    private final FileSystemWrapper filesystem;

    @Inject
    public StorageReadinessCheck(FileSystemWrapper filesystem) {
        this.filesystem = filesystem;
    }

    @Override
    public HealthCheckResponse call() {
        try {
            filesystem.save(PROBE_PATH, new ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8)));
            filesystem.delete(PROBE_PATH);
            return HealthCheckResponse.up("storage");
        } catch (IOException | RuntimeException e) {
            return HealthCheckResponse.named("storage")
                    .down()
                    .withData("error", e.toString())
                    .build();
        }
    }
}
