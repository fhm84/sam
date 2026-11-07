package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** A download by IDs stays bounded, whatever the request asks for. */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class BatchDownloadLimitsTest {

    private String sheetId;
    private String attachmentId;

    @BeforeEach
    void createSheetWithDocument() {
        sheetId = given().contentType(ContentType.JSON)
                .body(Map.of("title", "Batch limits " + UUID.randomUUID()))
                .post("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        byte[] pdf = ("%PDF-1.4\n% " + UUID.randomUUID() + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII);
        attachmentId = given().multiPart("file", "part.pdf", pdf, "application/pdf")
                .multiPart("type", "FULL_SCORE")
                .post("/api/sheets/{id}/documents", sheetId)
                .then()
                .statusCode(200)
                .extract()
                .path("attachment.id");
    }

    @AfterEach
    void deleteSheet() {
        given().delete("/api/sheets/{id}", sheetId);
    }

    private static List<String> randomIds(int count) {
        return Stream.generate(() -> UUID.randomUUID().toString()).limit(count).toList();
    }

    @Test
    void moreDocumentsThanTheLimitAreRefused() {
        given().contentType(ContentType.JSON)
                .body(Map.of("ids", randomIds(201), "format", "MERGED_PDF"))
                .post("/api/documents/batch")
                .then()
                .statusCode(400);
    }

    @Test
    void theLimitItselfIsAllowed() {
        // None of them exist, so there is nothing to send, but the request is accepted
        given().contentType(ContentType.JSON)
                .body(Map.of("ids", randomIds(200), "format", "ZIP"))
                .post("/api/documents/batch")
                .then()
                .statusCode(204);
    }

    @Test
    void repeatedIdsCountOnce() throws Exception {
        List<String> ids = new ArrayList<>(List.of(attachmentId));
        for (int i = 0; i < 300; i++) {
            ids.add(attachmentId);
        }

        byte[] zip = given().contentType(ContentType.JSON)
                .body(Map.of("ids", ids, "format", "ZIP"))
                .post("/api/documents/batch")
                .then()
                .statusCode(200)
                .extract()
                .asByteArray();

        int entries = 0;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                entries++;
            }
        }
        assertEquals(1, entries);
    }
}
