package de.halbmann.sam.storage;

import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.storage.api.FileSystemWrapper;
import de.halbmann.storage.local.LocalFileSystemProvider;
import de.halbmann.storage.spi.StorageLocation;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests for local storage paths. A relative base path like {@code target/uploads} used
 * to become {@code file://target/uploads} — "target" parsed as the URI host — so files were written
 * to {@code /uploads} at the filesystem root ({@code C:\\uploads} on Windows). And moving onto an
 * existing content-addressed file failed instead of replacing it.
 */
class LocalStorageLocationTest {

    private final LocalFileSystemProvider provider = new LocalFileSystemProvider();

    @TempDir
    Path tempDir;

    @Test
    void relativePath_resolvesAgainstWorkingDirectory() {
        StorageLocation location = StorageLocation.parse("target/uploads");

        assertTrue(provider.supports(location));
        FileSystemWrapper fs = provider.create(location);
        assertEquals(Path.of("target/uploads", "ab/file.pdf").toAbsolutePath().normalize(), fs.resolve("ab/file.pdf"));
    }

    @Test
    void absolutePath_isUsedAsIs() {
        FileSystemWrapper fs = provider.create(StorageLocation.parse(tempDir.toString()));

        assertEquals(tempDir.resolve("x.txt"), fs.resolve("x.txt"));
    }

    @Test
    void explicitFileUri_isStillSupported() {
        FileSystemWrapper fs =
                provider.create(StorageLocation.parse(tempDir.toUri().toString()));

        assertEquals(tempDir.resolve("x.txt"), fs.resolve("x.txt"));
    }

    @Test
    void fileUriWithHost_isRejectedInsteadOfMisread() {
        StorageLocation location = StorageLocation.parse("file://relative/dir");

        assertThrows(IllegalArgumentException.class, () -> provider.create(location));
    }

    @Test
    void s3Location_isUnchanged() {
        StorageLocation location = StorageLocation.parse("s3://my-bucket/some/prefix");

        assertEquals("s3", location.scheme());
        assertEquals("my-bucket", location.authority());
        assertEquals("/some/prefix", location.path());
    }

    @Test
    void move_replacesExistingTarget() throws Exception {
        FileSystemWrapper fs = provider.create(StorageLocation.parse(tempDir.toString()));
        fs.save("ab/cd/hash.pdf", new ByteArrayInputStream("leftover".getBytes(StandardCharsets.UTF_8)));
        fs.save(".upload-tmp/new.tmp", new ByteArrayInputStream("content".getBytes(StandardCharsets.UTF_8)));

        fs.move(".upload-tmp/new.tmp", "ab/cd/hash.pdf");

        assertEquals("content", Files.readString(tempDir.resolve("ab/cd/hash.pdf")));
        assertFalse(Files.exists(tempDir.resolve(".upload-tmp/new.tmp")));
    }
}
