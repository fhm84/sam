package de.halbmann.sam.classification;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

/**
 * Applying a classification with an unknown instrument name creates the instrument with an ID
 * derived from the name. Regression test: two different names with the same slug (e.g. "Alto
 * Horn" and "Alto-Horn") made the second apply fail with a duplicate primary key.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class ClassificationApplyInstrumentIdTest {

    @Test
    void sameSlugDifferentName_getsSuffixedId() {
        String word = randomWord();

        applyWithInstrument(uploadDocument(), "Alto " + word);
        applyWithInstrument(uploadDocument(), "Alto-" + word);

        assertEquals("Alto " + word, instrumentName("alto-" + word));
        assertEquals("Alto-" + word, instrumentName("alto-" + word + "-2"));
    }

    private void applyWithInstrument(String documentId, String instrumentName) {
        given().contentType(ContentType.JSON)
                .body("{\"title\":\"Sheet " + UUID.randomUUID() + "\",\"instrumentName\":\"" + instrumentName + "\"}")
                .post("/api/documents/{id}/apply", documentId)
                .then()
                .statusCode(200);
    }

    private String uploadDocument() {
        byte[] content = ("part " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        return given().multiPart("file", "part.txt", content, "text/plain")
                .post("/api/documents")
                .then()
                .statusCode(200)
                .extract()
                .path("document.id");
    }

    private String instrumentName(String instrumentId) {
        return given().get("/api/instruments/{id}", instrumentId)
                .then()
                .statusCode(200)
                .extract()
                .path("name");
    }

    private static String randomWord() {
        StringBuilder sb = new StringBuilder("zq");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 8; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }
}
