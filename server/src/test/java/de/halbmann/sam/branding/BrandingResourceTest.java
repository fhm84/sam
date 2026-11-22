package de.halbmann.sam.branding;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Without configuration there is nothing to show, and the UI keeps SAM's own look. Public, without a
 * token: the share page shows the branding to guests. Configured: {@link BrandingResourceBrandedTest}.
 */
@QuarkusTest
class BrandingResourceTest {

    @Test
    void noNameNoLogo() {
        given().get("/api/public/branding")
                .then()
                .statusCode(200)
                .body("name", nullValue())
                .body("logoVersion", nullValue());
        given().get("/api/public/branding/logo").then().statusCode(404);
    }
}
