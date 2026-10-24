package de.halbmann.sam.storage;

import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.storage.local.LocalFileSystemProvider;
import de.halbmann.storage.spi.StorageLocation;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StorageReadinessCheckTest {

    private final LocalFileSystemProvider provider = new LocalFileSystemProvider();

    @TempDir
    Path tempDir;

    @Test
    void writableStorage_isUp_andLeavesNoProbeBehind() {
        StorageReadinessCheck check =
                new StorageReadinessCheck(provider.create(StorageLocation.parse(tempDir.toString())));

        HealthCheckResponse response = check.call();

        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        assertFalse(Files.exists(tempDir.resolve(StorageReadinessCheck.PROBE_PATH)));
    }

    @Test
    void unwritableStorage_isDown() throws Exception {
        // A regular file where the storage directory should be: the probe can't be written below it.
        Path notADirectory = Files.createFile(tempDir.resolve("not-a-directory"));
        StorageReadinessCheck check =
                new StorageReadinessCheck(provider.create(StorageLocation.parse(notADirectory.toString())));

        HealthCheckResponse response = check.call();

        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
        assertTrue(response.getData().orElseThrow().containsKey("error"));
    }
}
