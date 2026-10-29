package de.halbmann.sam.business.persons.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

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
 * Composer/arranger names are resolved to persons by exact name, but {@code persons.name}
 * isn't unique. Regression test: once two persons shared a name, every sheet save naming them
 * failed with a non-unique-result 500; and an untrimmed lookup next to a trimmed create turned
 * {@code "Name "} into a fresh duplicate on every save.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class PersonNameLookupRegressionTest {

    private final List<String> createdSheetIds = new ArrayList<>();

    @AfterEach
    void deleteCreatedSheets() {
        createdSheetIds.forEach(id -> given().delete("/api/sheets/{id}", id));
        createdSheetIds.clear();
    }

    @Test
    void sheetSave_withDuplicateComposerName_resolvesToOldestPerson() {
        String name = "Duplicate Composer " + UUID.randomUUID();
        String oldestId = createPerson(name);
        createPerson(name);

        String sheetId = createSheetWithComposer(name);

        assertEquals(oldestId, composerIdOf(sheetId));
    }

    @Test
    void sheetSave_withTrailingWhitespace_reusesTheSamePerson() {
        String name = "Spaced Composer " + UUID.randomUUID();

        String first = createSheetWithComposer(name + "  ");
        String second = createSheetWithComposer(name + "  ");

        String composerId = composerIdOf(first);
        assertNotNull(composerId);
        assertEquals(composerId, composerIdOf(second));
        assertEquals(
                name,
                given().get("/api/persons/{id}", composerId).then().extract().path("name"));
    }

    private String createPerson(String name) {
        Person person = new Person();
        person.setName(name);
        return given().contentType(ContentType.JSON)
                .body(person)
                .post("/api/persons")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
    }

    private String createSheetWithComposer(String composerName) {
        Person composer = new Person();
        composer.setName(composerName);
        SheetMusic sheet = new SheetMusic();
        sheet.setTitle("Sheet " + UUID.randomUUID());
        sheet.setComposer(composer);
        String id = given().contentType(ContentType.JSON)
                .body(sheet)
                .post("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        createdSheetIds.add(id);
        return id;
    }

    private String composerIdOf(String sheetId) {
        return given().get("/api/sheets/{id}", sheetId)
                .then()
                .statusCode(200)
                .extract()
                .path("composer.id");
    }
}
