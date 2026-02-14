package de.halbmann.sam.cli.controller;

import de.halbmann.sam.cli.entity.ImportResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

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