package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Share creation publishes a resource to anyone holding the link, so it must be restricted to
 * write roles and must reject references to resources that don't exist.
 *
 * <p>The success path isn't covered here: {@code @TestSecurity} provides no JWT, so
 * {@code CurrentUserService.getUserId()} is {@code null} and the {@code NOT NULL} creator column
 * can't be filled.
 */
@QuarkusTest
class SharesResourceSecurityTest {

    private static String shareBody(String type, UUID resourceId) {
        return "{\"resourceType\":\"" + type + "\",\"resourceId\":\"" + resourceId + "\"}";
    }

    @Test
    void unauthenticated_create_returns401() {
        given().contentType(ContentType.JSON)
                .body(shareBody("SHEET", UUID.randomUUID()))
                .post("/api/shares")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void authenticated_noRole_create_returns403() {
        given().contentType(ContentType.JSON)
                .body(shareBody("COLLECTION", UUID.randomUUID()))
                .post("/api/shares")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void authenticated_noRole_canStillListOwnShares() {
        given().get("/api/shares").then().statusCode(200);
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void librarian_create_unknownSheet_returns404() {
        given().contentType(ContentType.JSON)
                .body(shareBody("SHEET", UUID.randomUUID()))
                .post("/api/shares")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void librarian_create_unknownCollection_returns404() {
        given().contentType(ContentType.JSON)
                .body(shareBody("COLLECTION", UUID.randomUUID()))
                .post("/api/shares")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void librarian_create_unknownInstrumentation_returns404() {
        given().contentType(ContentType.JSON)
                .body(shareBody("INSTRUMENTATION", UUID.randomUUID()))
                .post("/api/shares")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void librarian_create_missingResourceId_returns400() {
        given().contentType(ContentType.JSON)
                .body("{\"resourceType\":\"SHEET\"}")
                .post("/api/shares")
                .then()
                .statusCode(400);
    }
}
