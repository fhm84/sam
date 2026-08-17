package de.halbmann.sam.api.impl;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * The suggest-items body is bean-validated ({@code @Valid} on the API interface): a missing/blank
 * goal must be a 400, not a 500 from the prompt template — and the oversized-goal cap protects the
 * LLM endpoint from cost amplification.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class SuggestItemsValidationTest {

    @Test
    void missingGoal_isRejectedWith400() {
        given().contentType(ContentType.JSON)
                .body("{}")
                .post(
                        "/api/sheet-collections/{id}/ai/suggest-items",
                        UUID.randomUUID().toString())
                .then()
                .statusCode(400);
    }

    @Test
    void oversizedGoal_isRejectedWith400() {
        String hugeGoal = "x".repeat(2001);
        given().contentType(ContentType.JSON)
                .body("{\"goal\":\"" + hugeGoal + "\"}")
                .post(
                        "/api/sheet-collections/{id}/ai/suggest-items",
                        UUID.randomUUID().toString())
                .then()
                .statusCode(400);
    }
}
