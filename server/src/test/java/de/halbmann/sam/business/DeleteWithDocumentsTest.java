package de.halbmann.sam.business;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.business.documents.boundary.AttachmentRepository;
import de.halbmann.sam.business.documents.boundary.DocumentRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression: deleting a sheet or an instrumentation that has documents answered 500, because the
 * attachments were deleted while the owner's collection still held them. The documents stay in the
 * upload pool (reference count down), only the attachments go.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class DeleteWithDocumentsTest {

    @Inject
    AttachmentRepository attachmentRepository;

    @Inject
    DocumentRepository documentRepository;

    private String instrumentId;
    private String sheetId;
    private String instrumentationId;

    @BeforeEach
    void createSheetWithInstrumentation() {
        instrumentId =
                "DELETE_TEST_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        given().contentType(ContentType.JSON)
                .body(Map.of("id", instrumentId, "name", "Delete Test"))
                .post("/api/instruments")
                .then()
                .statusCode(200);
        sheetId = given().contentType(ContentType.JSON)
                .body(Map.of("title", "Delete " + UUID.randomUUID()))
                .post("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        given().contentType(ContentType.JSON)
                .body(Map.of("instrumentId", instrumentId))
                .post("/api/sheets/{id}/instrumentations", sheetId)
                .then()
                .statusCode(204);
        instrumentationId = given().get("/api/sheets/{id}/instrumentations", sheetId)
                .then()
                .statusCode(200)
                .extract()
                .path("[0].id");
    }

    @AfterEach
    void cleanUp() {
        given().delete("/api/sheets/{id}", sheetId);
        given().delete("/api/instruments/{id}", instrumentId);
    }

    private String upload(String path, String type) {
        byte[] content = ("delete test " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        return given().multiPart("file", "part.txt", content, "text/plain")
                .multiPart("type", type)
                .post(path)
                .then()
                .statusCode(200)
                .extract()
                .path("attachment.id");
    }

    private boolean attachmentExists(String id) {
        return QuarkusTransaction.requiringNew()
                .call(() -> attachmentRepository
                        .findByIdOptional(UUID.fromString(id))
                        .isPresent());
    }

    @Test
    void deleteSheet_withSheetAndPartDocuments_succeeds() {
        String score = upload("/api/sheets/" + sheetId + "/documents", "FULL_SCORE");
        String part =
                upload("/api/sheets/" + sheetId + "/instrumentations/" + instrumentationId + "/documents", "PART");

        given().delete("/api/sheets/{id}", sheetId).then().statusCode(204);

        given().get("/api/sheets/{id}", sheetId).then().statusCode(404);
        assertFalse(attachmentExists(score), "sheet attachment removed");
        assertFalse(attachmentExists(part), "instrumentation attachment removed");
    }

    @Test
    void deleteInstrumentation_withDocument_succeeds_andKeepsTheDocumentInThePool() {
        String part =
                upload("/api/sheets/" + sheetId + "/instrumentations/" + instrumentationId + "/documents", "PART");
        UUID documentId = QuarkusTransaction.requiringNew()
                .call(() -> attachmentRepository
                        .findById(UUID.fromString(part))
                        .getDocument()
                        .getId());

        given().delete("/api/sheets/{s}/instrumentations/{i}", sheetId, instrumentationId)
                .then()
                .statusCode(204);

        assertFalse(attachmentExists(part));
        QuarkusTransaction.requiringNew().run(() -> {
            var document = documentRepository.findById(documentId);
            assertNotNull(document, "the document stays in the upload pool");
            assertEquals(0, document.getRefCount());
        });
    }
}
