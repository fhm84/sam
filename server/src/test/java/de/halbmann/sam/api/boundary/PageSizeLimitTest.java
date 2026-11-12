package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The {@code size} query parameter: everyone may ask for pages up to the limit; larger pages and
 * "no paging" ({@code size=-1}) are for music librarians and admins only (their exports and the
 * {@code cli} use them). See {@code PageSizeLimitFilter}.
 */
@QuarkusTest
class PageSizeLimitTest {

    private static final String[] LISTS = {"/api/sheets", "/api/persons", "/api/instruments", "/api/ensembles"};

    @ParameterizedTest
    @ValueSource(strings = {"-1", "501", "2147483647", "-5"})
    @TestSecurity(
            user = "musician",
            roles = {})
    void plainUser_isRefusedPagesOverTheLimit(String size) {
        for (String list : LISTS) {
            given().queryParam("size", size)
                    .when()
                    .get(list)
                    .then()
                    .statusCode(400)
                    .body("message", containsString("size must be between 0 and 500"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "10", "200", "500"})
    @TestSecurity(
            user = "musician",
            roles = {})
    void plainUser_getsPagesUpToTheLimit(String size) {
        for (String list : LISTS) {
            given().queryParam("size", size).when().get(list).then().statusCode(200);
        }
    }

    @Test
    @TestSecurity(
            user = "musician",
            roles = {})
    void plainUser_withoutSizeGetsTheDefaultPage() {
        for (String list : LISTS) {
            given().when().get(list).then().statusCode(200);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "501", "5000"})
    @TestSecurity(
            user = "librarian",
            roles = {"music_librarian"})
    void librarian_mayReadWholeLists(String size) {
        for (String list : LISTS) {
            given().queryParam("size", size).when().get(list).then().statusCode(200);
        }
    }

    @Test
    @TestSecurity(
            user = "admin",
            roles = {"admin"})
    void admin_mayReadWholeLists() {
        for (String list : LISTS) {
            given().queryParam("size", "-1").when().get(list).then().statusCode(200);
        }
    }
}
