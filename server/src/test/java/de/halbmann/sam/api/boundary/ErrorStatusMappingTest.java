package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.path.json.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Client errors must come back as 4xx, not as 500. Regression test: rejected uploads, a malformed
 * ID in the path and deleting a still-linked document all used to return 500.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class ErrorStatusMappingTest {

    @Test
    void emptyUpload_returns400() {
        given().multiPart("file", "empty.txt", new byte[0], "text/plain")
                .post("/api/documents")
                .then()
                .statusCode(400)
                .body("message", containsString("empty"));
    }

    @Test
    void pdfWithWrongExtension_returns400() {
        byte[] pdf = ("%PDF-1.4\n% " + UUID.randomUUID() + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII);

        given().multiPart("file", "scan.txt", pdf, "text/plain")
                .post("/api/documents")
                .then()
                .statusCode(400)
                .body("message", containsString("PDF"));
    }

    @Test
    void malformedIdInPath_returns400() {
        given().get("/api/sheets/not-a-uuid").then().statusCode(400).body("message", containsString("Malformed ID"));
    }

    @Test
    void deletingStillLinkedDocument_returns409() {
        byte[] content = ("linked " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        JsonPath upload = given().multiPart("file", "part.txt", content, "text/plain")
                .multiPart("type", "PART")
                .post("/api/documents")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        given().delete("/api/documents/{id}", upload.getString("document.id"))
                .then()
                .statusCode(409)
                .body("message", containsString("still linked"));
    }
}
