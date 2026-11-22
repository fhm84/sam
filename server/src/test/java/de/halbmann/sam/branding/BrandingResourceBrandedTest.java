package de.halbmann.sam.branding;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

/** With a name and a logo file configured, both are served without a token. */
@QuarkusTest
@TestProfile(BrandedProfile.class)
class BrandingResourceBrandedTest {

    @Test
    void servesNameAndVersionedLogo() {
        String version = given().get("/api/public/branding")
                .then()
                .statusCode(200)
                .body("name", equalTo("Test Band"))
                .extract()
                .path("logoVersion");

        byte[] body = given().queryParam("v", version)
                .get("/api/public/branding/logo")
                .then()
                .statusCode(200)
                .contentType("image/png")
                .header("Cache-Control", "public, max-age=31536000, immutable")
                .header("Content-Security-Policy", BrandingResource.LOGO_CSP)
                .extract()
                .asByteArray();
        assertArrayEquals(BrandedProfile.LOGO, body);
    }

    @Test
    void staleOrMissingVersionIsRevalidated() {
        given().queryParam("v", "old")
                .get("/api/public/branding/logo")
                .then()
                .statusCode(200)
                .header("Cache-Control", "no-cache");
        given().get("/api/public/branding/logo").then().statusCode(200).header("Cache-Control", "no-cache");
    }
}
