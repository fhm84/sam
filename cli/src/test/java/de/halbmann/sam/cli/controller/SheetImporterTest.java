package de.halbmann.sam.cli.controller;

import de.halbmann.sam.cli.entity.ImportResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SheetImporterTest {

    @Inject
    SheetImporter sheetImporter;

    @Test
    void testCreateFromSheet() {
        Path path = Paths.get("", "src", "test", "resources", "data", "A_Banda.json");
        ImportResult importResult = sheetImporter.importFile(path.toFile(), false);
    }
}
