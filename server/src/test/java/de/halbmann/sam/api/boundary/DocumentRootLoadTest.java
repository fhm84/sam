package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.path.json.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * {@code GET /api/documents/{id}} accepts an attachment ID or a document ID. Regression test: the
 * document fallback was evaluated eagerly ({@code orElse}), so every attachment ID ended in a
 * "Document not found" 404.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class DocumentRootLoadTest {

    @Test
    void loadByAttachmentId_andByDocumentId_bothServeTheFile() {
        String content = "root load " + UUID.randomUUID();
        JsonPath upload = given().multiPart("file", "notes.txt", content.getBytes(StandardCharsets.UTF_8), "text/plain")
                .multiPart("type", "PART")
                .post("/api/documents")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();
        String attachmentId = upload.getString("attachment.id");
        String documentId = upload.getString("document.id");
        assertNotNull(attachmentId);

        String byAttachment = given().get("/api/documents/{id}", attachmentId)
                .then()
                .statusCode(200)
                .extract()
                .asString();
        String byDocument = given().get("/api/documents/{id}", documentId)
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertEquals(content, byAttachment);
        assertEquals(content, byDocument);
    }
}
