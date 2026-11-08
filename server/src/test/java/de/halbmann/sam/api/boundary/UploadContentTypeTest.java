package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Uploads are checked on the type detected from their content, with the default allow-list from
 * {@code application.properties}; the file name doesn't help a web page or a program through.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class UploadContentTypeTest {

    private static final String HTML = "<!DOCTYPE html><html><body><p>hello</p></body></html>";
    private static final String SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\"><circle r=\"1\"/></svg>";
    private static final String EXE = "MZ\u0090\0\u0003\0\0\0\u0004\0\0\0ÿÿ\0\0";

    private static byte[] bytes(String content) {
        // A unique tail keeps the upload from deduplicating against an earlier test's file
        return (content + "\n<!-- " + UUID.randomUUID() + " -->").getBytes(StandardCharsets.ISO_8859_1);
    }

    @ParameterizedTest
    @CsvSource({"page.html,HTML", "page.pdf,HTML", "notes.txt,HTML", "icon.svg,SVG", "scan.png,SVG", "tool.exe,EXE"})
    void webPagesImagesWithScriptAndPrograms_areRefused(String filename, String kind) {
        String content =
                switch (kind) {
                    case "HTML" -> HTML;
                    case "SVG" -> SVG;
                    default -> EXE;
                };
        given().multiPart("file", filename, bytes(content), "application/octet-stream")
                .post("/api/documents")
                .then()
                .statusCode(400);
    }

    @ParameterizedTest
    @CsvSource({"scan.pdf,%PDF-1.7", "notes.txt,Programme note: a march in B flat."})
    void scansAndText_areAccepted_andServedWithNosniff(String filename, String content) {
        String documentId = given().multiPart("file", filename, bytes(content), "application/octet-stream")
                .post("/api/documents")
                .then()
                .statusCode(200)
                .extract()
                .path("document.id");
        given().get("/api/documents/{id}", documentId)
                .then()
                .statusCode(200)
                .header("X-Content-Type-Options", equalTo("nosniff"))
                .header("Content-Type", containsString(filename.endsWith(".pdf") ? "application/pdf" : "text/plain"));
        given().delete("/api/documents/{id}", documentId).then().statusCode(204);
    }
}
