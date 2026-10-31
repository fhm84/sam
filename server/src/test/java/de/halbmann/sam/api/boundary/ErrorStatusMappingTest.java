package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Client errors must come back as 4xx, not as 500. Regression test: rejected uploads, a malformed
 * ID in the path, deleting a still-linked document, a body of the wrong JSON shape and an empty
 * create request all used to return 500.
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

    private static final String ID = "00000000-0000-0000-0000-000000000001";

    @ParameterizedTest
    @ValueSource(
            strings = {
                "POST /api/sheets/" + ID + "/tags",
                "DELETE /api/sheets/" + ID + "/tags",
                "POST /api/sheets/" + ID + "/instrumentations/bulk",
                "PUT /api/sheet-collections/" + ID + "/items/order"
            })
    void objectWhereListIsExpected_returns400(String endpoint) {
        String[] parts = endpoint.split(" ", 2);
        given().contentType(ContentType.JSON)
                .body("{}")
                .request(parts[0], parts[1])
                .then()
                .statusCode(400)
                .body("message", containsString("Malformed request body"));
    }

    @ParameterizedTest
    @CsvSource({"/api/sheets, title", "/api/ensembles, name", "/api/instruments, id", "/api/instruments, name"})
    void emptyCreateRequest_returns400NamingTheField(String path, String field) {
        given().contentType(ContentType.JSON)
                .body("{}")
                .post(path)
                .then()
                .statusCode(400)
                .body("violations.field", hasItem(endsWith("." + field)));
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
