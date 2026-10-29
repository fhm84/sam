package de.halbmann.sam.business.persons.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.persons.Person;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The person name search treats {@code %} and {@code _} literally. Regression test: the input
 * went into the prefix/substring {@code LIKE} unescaped, so searching for {@code %} found every
 * person.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class PersonNameSearchWildcardTest {

    private final List<String> createdIds = new ArrayList<>();

    @AfterEach
    void deleteCreatedData() {
        createdIds.forEach(id -> given().delete("/api/persons/{id}", id));
        createdIds.clear();
    }

    @Test
    void nameSearch_treatsWildcardCharactersLiterally() {
        String suffix = UUID.randomUUID().toString();
        String percent = createPerson("Trio 100% " + suffix);
        String underscore = createPerson("Duo_Nord " + suffix);
        String plain = createPerson("Plain Name " + suffix);

        List<String> percentHits = searchIds("%");
        assertTrue(percentHits.contains(percent));
        assertFalse(percentHits.contains(underscore));
        assertFalse(percentHits.contains(plain));

        List<String> underscoreHits = searchIds("_");
        assertTrue(underscoreHits.contains(underscore));
        assertFalse(underscoreHits.contains(percent));
        assertFalse(underscoreHits.contains(plain));
    }

    @Test
    void nameSearch_stillFindsPlainSubstrings() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String person = createPerson("Ernst Mosch " + suffix);

        assertTrue(searchIds("mosch " + suffix).contains(person));
    }

    private List<String> searchIds(String name) {
        return given().queryParam("name", name)
                .queryParam("size", 1000)
                .get("/api/persons")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("data.id", String.class);
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
        createdIds.add(id);
        return id;
    }
}
