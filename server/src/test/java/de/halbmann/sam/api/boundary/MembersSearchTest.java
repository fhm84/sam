package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import de.halbmann.sam.api.entity.members.Member;
import de.halbmann.sam.api.entity.members.MembershipStatus;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The member list ({@code GET /api/members}) searches names like the person search (substring,
 * trigram, phonetic), filters by status and sorts by name in either direction.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class MembersSearchTest {

    private final List<String> createdIds = new ArrayList<>();

    @AfterEach
    void deleteCreatedMembers() {
        createdIds.forEach(id -> given().delete("/api/members/{id}", id));
        createdIds.clear();
    }

    @Test
    void nameSearch_toleratesTypos() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String id = createMember("Wolfgang Mozart " + suffix, MembershipStatus.ACTIVE);

        search("Mosart " + suffix).body("data.id", hasItem(id));
    }

    @Test
    void nameSearch_combinesWithStatus() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String active = createMember("Status Test " + suffix, MembershipStatus.ACTIVE);
        String former = createMember("Status Test Former " + suffix, MembershipStatus.FORMER);

        given().queryParam("name", "Status Test")
                .queryParam("status", "FORMER")
                .queryParam("size", -1)
                .get("/api/members")
                .then()
                .statusCode(200)
                .body("data.id", hasItem(former))
                .body("data.id", not(hasItem(active)));
    }

    @Test
    void sortByNameDescending_reversesTheOrder() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        createMember("Aaron Sort " + suffix, MembershipStatus.ACTIVE);
        createMember("Zora Sort " + suffix, MembershipStatus.ACTIVE);

        given().queryParam("name", "Sort " + suffix)
                .queryParam("sortBy", "name")
                .queryParam("sortOrder", "DESC")
                .get("/api/members")
                .then()
                .statusCode(200)
                .body("data[0].name", startsWith("Zora"));
    }

    @Test
    void sortByAnotherField_isRejected() {
        given().queryParam("sortBy", "email").get("/api/members").then().statusCode(400);
    }

    private io.restassured.response.ValidatableResponse search(String name) {
        return given().queryParam("name", name)
                .queryParam("size", -1)
                .get("/api/members")
                .then()
                .statusCode(200);
    }

    private String createMember(String name, MembershipStatus status) {
        Member member = new Member();
        member.setName(name);
        member.setStatus(status);
        String id = given().contentType(ContentType.JSON)
                .body(member)
                .post("/api/members")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        createdIds.add(id);
        return id;
    }
}
