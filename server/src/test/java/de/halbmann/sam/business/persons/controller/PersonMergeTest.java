package de.halbmann.sam.business.persons.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import de.halbmann.sam.api.entity.members.Member;
import de.halbmann.sam.api.entity.persons.Person;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Merging a duplicate person ({@code POST /api/persons/{id}/merge-into/{targetId}}) moves its
 * credits to the kept person; a member profile always stays with its record.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class PersonMergeTest {

    private final List<String> sheetIds = new ArrayList<>();
    private final List<String> memberIds = new ArrayList<>();
    private final List<String> personIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        sheetIds.forEach(id -> given().delete("/api/sheets/{id}", id));
        memberIds.forEach(id -> given().delete("/api/members/{id}", id));
        personIds.forEach(id -> given().delete("/api/persons/{id}", id));
    }

    @Test
    void merge_movesCompositionsAndArrangementsToTheKeptPerson() {
        String duplicate = createPerson("J. Merge " + UUID.randomUUID());
        String keep = createPerson("Johann Merge " + UUID.randomUUID());
        String composed = createSheet(duplicate, null);
        String arranged = createSheet(null, duplicate);

        given().post("/api/persons/{id}/merge-into/{target}", duplicate, keep)
                .then()
                .statusCode(200)
                .body("id", equalTo(keep));

        given().get("/api/persons/{id}", duplicate).then().statusCode(404);
        given().get("/api/sheets/{id}", composed).then().body("composer.id", equalTo(keep));
        given().get("/api/sheets/{id}", arranged).then().body("arranger.id", equalTo(keep));
        given().get("/api/persons/{id}/credits", keep).then().body("role", containsInAnyOrder("COMPOSER", "ARRANGER"));
    }

    @Test
    void merge_ofAMember_keepsTheProfileAndTakesOverTheChosenData() {
        String keepName = "Johann Member " + UUID.randomUUID();
        String member = createMember("J. Member " + UUID.randomUUID());
        String keep = createPerson(keepName);
        String sheet = createSheet(keep, null);

        given().post("/api/persons/{id}/merge-into/{target}", member, keep)
                .then()
                .statusCode(200)
                .body("id", equalTo(member))
                .body("name", equalTo(keepName));

        given().get("/api/persons/{id}", keep).then().statusCode(404);
        given().get("/api/members/{id}", member).then().statusCode(200).body("name", equalTo(keepName));
        given().get("/api/sheets/{id}", sheet).then().body("composer.id", equalTo(member));
    }

    @Test
    void merge_ofTwoMembers_isAConflict() {
        String a = createMember("Member A " + UUID.randomUUID());
        String b = createMember("Member B " + UUID.randomUUID());

        given().post("/api/persons/{id}/merge-into/{target}", a, b).then().statusCode(409);
    }

    @Test
    void merge_intoItself_isRejected() {
        String a = createPerson("Self " + UUID.randomUUID());

        given().post("/api/persons/{id}/merge-into/{target}", a, a).then().statusCode(400);
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void merge_withoutWriteRole_isForbidden() {
        given().post("/api/persons/{id}/merge-into/{target}", UUID.randomUUID(), UUID.randomUUID())
                .then()
                .statusCode(403);
    }

    private String createPerson(String name) {
        Person person = new Person();
        person.setName(name);
        String id = given().contentType(ContentType.JSON)
                .body(person)
                .post("/api/persons")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        personIds.add(id);
        return id;
    }

    private String createMember(String name) {
        Member member = new Member();
        member.setName(name);
        String id = given().contentType(ContentType.JSON)
                .body(member)
                .post("/api/members")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        memberIds.add(id);
        return id;
    }

    private String createSheet(String composerId, String arrangerId) {
        SheetMusic sheet = new SheetMusic();
        sheet.setTitle("Merge Test " + UUID.randomUUID());
        if (composerId != null) {
            Person composer = new Person();
            composer.setId(UUID.fromString(composerId));
            sheet.setComposer(composer);
        }
        if (arrangerId != null) {
            Person arranger = new Person();
            arranger.setId(UUID.fromString(arrangerId));
            sheet.setArranger(arranger);
        }
        String id = given().contentType(ContentType.JSON)
                .body(sheet)
                .post("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        sheetIds.add(id);
        return id;
    }
}
