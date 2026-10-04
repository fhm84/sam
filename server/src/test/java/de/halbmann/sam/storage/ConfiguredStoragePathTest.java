package de.halbmann.sam.storage;

import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.storage.api.FileSystemWrapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * The test profile stores uploads under {@code target/uploads} (relative to the module). Before the
 * fix, that resolved to {@code /uploads} at the filesystem root — a permission error on Linux CI
 * and stray files in {@code C:\\uploads} on Windows.
 */
@QuarkusTest
class ConfiguredStoragePathTest {

    @Inject
    FileSystemWrapper filesystem;

    @Test
    void testStorage_resolvesInsideTheModulesTargetDirectory() {
        Path expectedBase = Path.of("target/uploads").toAbsolutePath().normalize();

        assertTrue(
                filesystem.resolve("probe.txt").startsWith(expectedBase),
                filesystem.resolve("probe.txt") + " is not under " + expectedBase);
    }
}
