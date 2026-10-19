package de.halbmann.sam.api.impl;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

/** Without a Pocket ID API key (the test default), search is off and users are linked by ID. */
@QuarkusTest
class AdminUsersResourceWithoutKeyTest {

    private static final String USER_ID = "6f1c2a3b-0000-4000-8000-000000000001";

    @Test
    @TestSecurity(user = "admin1", roles = "admin")
    void directory_isNotSearchable() {
        given().get("/api/admin/users/directory")
                .then()
                .statusCode(200)
                .body("provider", equalTo("pocket-id"))
                .body("searchable", equalTo(false));
    }

    @Test
    @TestSecurity(user = "admin1", roles = "admin")
    void search_isNotImplemented() {
        given().queryParam("search", "alice").get("/api/admin/users").then().statusCode(501);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "admin")
    void getUser_echoesTheId() {
        given().get("/api/admin/users/" + USER_ID)
                .then()
                .statusCode(200)
                .body("id", equalTo(USER_ID))
                .body("username", equalTo(USER_ID));
    }
}
