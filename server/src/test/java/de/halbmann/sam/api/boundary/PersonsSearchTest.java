package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import de.halbmann.sam.api.entity.persons.Person;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the person name search endpoint ({@code GET /api/persons?name=...}).
 *
 * <p>Verifies that the multi-signal search (prefix wildcard + word_similarity trigram + phonetic)
 * finds persons correctly under realistic user-input conditions such as partial prefixes, middle
 * fragments, typos, and different casing.
 *
 * <p>All tests run as {@code music_librarian} (class-level {@code @TestSecurity}) so they can both
 * create test data and query it. The database is shared within a test run; assertions use
 * {@code hasItem} so accumulated data from other test classes does not cause false failures.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class PersonsSearchTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private void createPerson(String name) throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            Person person = new Person();
            person.setName(name);
            given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(person))
                    .post("/api/persons")
                    .then()
                    .statusCode(200);
        }
    }

    private io.restassured.response.ValidatableResponse searchByName(String name) {
        return given().queryParam("name", name)
                .queryParam("size", -1) // disable paging so all results are returned
                .get("/api/persons")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    // ── exact & full-name ─────────────────────────────────────────────────────

    @Test
    void exactNameSearch_findsPerson() throws Exception {
        createPerson("Johann Sebastian Bach");

        searchByName("Johann Sebastian Bach").body("data.name", hasItem("Johann Sebastian Bach"));
    }

    // ── prefix / partial input ────────────────────────────────────────────────

    @Test
    void partialPrefixSearch_findsPerson() throws Exception {
        // "Mozar" is not a complete word — relies on ILIKE '%mozar%' matching "…Mozart"
        createPerson("Wolfgang Amadeus Mozart");

        searchByName("Mozar").body("data.name", hasItem("Wolfgang Amadeus Mozart"));
    }

    @Test
    void lastNamePrefixSearch_findsPerson() throws Exception {
        createPerson("Franz Schubert");

        searchByName("Schub").body("data.name", hasItem("Franz Schubert"));
    }

    @Test
    void firstNameSearch_findsPerson() throws Exception {
        createPerson("Franz Liszt");

        searchByName("Franz").body("data.name", hasItem("Franz Liszt"));
    }

    // ── substring / middle fragment ───────────────────────────────────────────

    @Test
    void middleNameSearch_findsPerson() throws Exception {
        // "Amadeus" is neither the first nor last name
        createPerson("Wolfgang Amadeus Mozart");

        searchByName("Amadeus").body("data.name", hasItem("Wolfgang Amadeus Mozart"));
    }

    @Test
    void prepositionPlusLastName_findsPerson() throws Exception {
        createPerson("Ludwig van Beethoven");

        searchByName("van Bee").body("data.name", hasItem("Ludwig van Beethoven"));
    }

    // ── case insensitivity ────────────────────────────────────────────────────

    @Test
    void uppercaseSearch_findsPerson() throws Exception {
        createPerson("Franz Schubert");

        searchByName("SCHUBERT").body("data.name", hasItem("Franz Schubert"));
    }

    @Test
    void lowercaseSearch_findsPerson() throws Exception {
        createPerson("Ludwig van Beethoven");

        searchByName("beethoven").body("data.name", hasItem("Ludwig van Beethoven"));
    }

    // ── fuzzy / typo tolerance ────────────────────────────────────────────────

    @Test
    void typoSearch_singleCharacterDrop_findsPerson() throws Exception {
        // "Bethoven" is missing one 'e' from "Beethoven" — caught by word_similarity
        createPerson("Ludwig van Beethoven");

        searchByName("Bethoven").body("data.name", hasItem("Ludwig van Beethoven"));
    }

    @Test
    void typoSearch_transposedCharacters_findsPerson() throws Exception {
        // "Mosart" swaps 'z' and 's' compared to "Mozart"
        createPerson("Wolfgang Amadeus Mozart");

        searchByName("Mosart").body("data.name", hasItem("Wolfgang Amadeus Mozart"));
    }

    // ── no match ─────────────────────────────────────────────────────────────

    @Test
    void noMatchSearch_returnsEmptyResult() {
        searchByName("xyzxyz123abc").body("totalCount", equalTo(0)).body("data", hasSize(0));
    }

    // ── ranking: prefix match ranks above weaker fuzzy match ─────────────────

    @Test
    void prefixMatchRanksBeforeFuzzyMatch() throws Exception {
        // "Liszt" is a direct substring match for "Liszt"
        // "Lista" has weak trigram overlap but should score lower
        createPerson("Franz Liszt");
        createPerson("Lista Noname"); // fictional; low similarity to "Liszt"

        // The direct match must be the first result
        searchByName("Liszt").body("data[0].name", equalTo("Franz Liszt"));
    }
}
