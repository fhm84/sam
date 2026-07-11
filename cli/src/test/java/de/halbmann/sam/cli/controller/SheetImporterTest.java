package de.halbmann.sam.cli.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.halbmann.sam.cli.entity.ImportResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SheetImporterTest {

    @Inject
    SheetImporter sheetImporter;

    @Test
    void testCreateFromSheet() {
        // dry-run only: parses/validates the JSON, does not require a running SAM server
        Path path = Paths.get("", "src", "test", "resources", "test-sheet.json");
        ImportResult importResult = sheetImporter.importFile(path.toFile(), true);

        assertTrue(importResult.success());
        assertEquals(path.toFile(), importResult.file());
    }

    @Test
    void testFileNotFound() {
        File missing =
                Paths.get("", "src", "test", "resources", "does-not-exist.json").toFile();
        ImportResult importResult = sheetImporter.importFile(missing, true);

        assertFalse(importResult.success());
        assertEquals(ImportResult.Status.FAILED, importResult.status());
    }

    @Test
    void testValidationFailsWithoutTitle() {
        // bean validation runs locally even in dry-run; the fixture has no title (@NotBlank)
        Path path = Paths.get("", "src", "test", "resources", "invalid-sheet.json");
        ImportResult importResult = sheetImporter.importFile(path.toFile(), true);

        assertEquals(ImportResult.Status.FAILED, importResult.status());
    }
}
