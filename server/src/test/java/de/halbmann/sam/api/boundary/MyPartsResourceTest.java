package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@code GET /api/me/parts}.
 *
 * <p>Tests authentication enforcement and the empty-response path when no musician record is
 * linked to the authenticated user. OIDC is disabled in the test profile; {@code @TestSecurity}
 * provides mock identities. Because {@code @TestSecurity} does not inject a real JWT, {@code
 * CurrentUserService.getUserId()} returns {@code null}, which causes {@code MyPartsService} to
 * return an empty result immediately without any database access.
 */
@QuarkusTest
class MyPartsResourceTest {

    @Test
    void unauthenticated_returns401() {
        given().get("/api/me/parts").then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void authenticated_noLinkedMusician_returnsEmptyPaginatedResponse() {
        given().get("/api/me/parts")
                .then()
                .statusCode(200)
                .body("data", hasSize(0))
                .body("totalCount", equalTo(0));
    }
}
