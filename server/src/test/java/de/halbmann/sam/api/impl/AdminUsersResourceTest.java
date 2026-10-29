package de.halbmann.sam.api.impl;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import de.halbmann.sam.security.PocketIdStub;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import java.util.List;
import org.junit.jupiter.api.Test;

/** User search for member linking against a stubbed Pocket ID with an API key. */
@QuarkusTest
@TestProfile(AdminUsersResourceTest.WithPocketIdKey.class)
class AdminUsersResourceTest {

    public static class WithPocketIdKey implements QuarkusTestProfile {

        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(new TestResourceEntry(PocketIdStub.class));
        }
    }

    @Test
    @TestSecurity(user = "admin1", roles = "admin")
    void directory_isSearchable() {
        given().get("/api/admin/users/directory")
                .then()
                .statusCode(200)
                .body("provider", equalTo("pocket-id"))
                .body("searchable", equalTo(true));
    }

    @Test
    @TestSecurity(user = "admin1", roles = "admin")
    void search_returnsPocketIdUsers() {
        given().queryParam("search", "alice")
                .get("/api/admin/users")
                .then()
                .statusCode(200)
                .body("id", contains(PocketIdStub.ALICE_ID))
                .body("username", contains("alice"));
    }

    @Test
    @TestSecurity(user = "admin1", roles = "admin")
    void getUser_unknownId_is404() {
        given().get("/api/admin/users/6f1c2a3b-0000-4000-8000-0000000000ff")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "librarian1", roles = "music_librarian")
    void search_isAdminOnly() {
        given().queryParam("search", "alice").get("/api/admin/users").then().statusCode(403);
    }
}
