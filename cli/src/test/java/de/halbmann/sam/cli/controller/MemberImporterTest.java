package de.halbmann.sam.cli.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.halbmann.sam.cli.entity.ImportResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/** Dry-run only: parses and validates the JSON, does not require a running SAM server. */
@QuarkusTest
class MemberImporterTest {

    @Inject
    MemberImporter memberImporter;

    @Test
    void memberFile_isAccepted() {
        ImportResult result = memberImporter.importFile(
                Paths.get("", "src", "test", "resources", "test-member.json").toFile(), true);

        assertEquals(ImportResult.Status.IMPORTED, result.status());
    }

    @Test
    void personFile_isRejected() {
        // A person export parses as a member (unknown fields are ignored) but must not become one
        ImportResult result = memberImporter.importFile(
                Paths.get("", "src", "test", "resources", "test-person.json").toFile(), true);

        assertEquals(ImportResult.Status.FAILED, result.status());
    }
}
