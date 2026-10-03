package de.halbmann.sam;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Pins the path of the OIDC bootstrap endpoint. The Angular app fetches {@code /oidc-config.json};
 * in production the Caddy edge proxy ({@code docker/Caddyfile}) rewrites that to
 * {@code /api/oidc-config.json} because the JAX-RS application root is {@code /api}. If this
 * endpoint moves, the rewrite must move with it, or the app can't find Keycloak.
 */
@QuarkusTest
class OidcConfigResourceTest {

    @Test
    void servedUnauthenticatedUnderApiRoot() {
        given().get("/api/oidc-config.json")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("issuerUrl", not(emptyOrNullString()))
                .body("clientId", equalTo("sam-ui"));
    }
}
