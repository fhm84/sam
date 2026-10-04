package de.halbmann.sam.business.sheets.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.sheets.Genre;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@code GET /api/sheets?composer=...} must filter by the composer's name. Regression test: the
 * list path built the HQL {@code composer.name=:composer.name}, and a named parameter can't contain
 * a dot, so every request with the filter failed. With a search term ({@code q}) the filter was
 * ignored altogether.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class SheetComposerFilterRegressionTest {

    private final List<String> createdSheetIds = new ArrayList<>();

    @AfterEach
    void deleteCreatedData() {
        createdSheetIds.forEach(id -> given().delete("/api/sheets/{id}", id));
        createdSheetIds.clear();
    }

    @Test
    void list_withComposer_returnsOnlyThatComposersSheets() {
        String composer = "Composer " + UUID.randomUUID();
        String mine = createSheet("Mine " + UUID.randomUUID(), composer, Genre.MARCH);
        String other = createSheet("Other " + UUID.randomUUID(), "Someone Else " + UUID.randomUUID(), Genre.MARCH);

        JsonPath result = list("composer", composer);

        assertEquals(List.of(mine), result.getList("data.id", String.class));
        assertEquals(1, result.getLong("totalCount"));
        assertFalse(result.getList("data.id", String.class).contains(other));
    }

    @Test
    void list_withComposerAndGenre_combinesBothFilters() {
        String composer = "Composer " + UUID.randomUUID();
        String march = createSheet("March " + UUID.randomUUID(), composer, Genre.MARCH);
        createSheet("Waltz " + UUID.randomUUID(), composer, Genre.WALTZ);

        JsonPath result = list("composer", composer, "genre", Genre.MARCH.name());

        assertEquals(List.of(march), result.getList("data.id", String.class));
    }

    @Test
    void search_withComposer_returnsOnlyThatComposersSheets() {
        String token = randomWord();
        String composer = "Composer " + UUID.randomUUID();
        String mine = createSheet("Alpha " + token, composer, Genre.MARCH);
        createSheet("Beta " + token, "Someone Else " + UUID.randomUUID(), Genre.MARCH);

        JsonPath result = list("q", token, "composer", composer);

        assertEquals(List.of(mine), result.getList("data.id", String.class));
        assertEquals(1, result.getLong("totalCount"));
    }

    /** {@code params} are name/value pairs; page 0, size 20. */
    private JsonPath list(Object... params) {
        var request = given().queryParam("page", 0).queryParam("size", 20);
        for (int i = 0; i < params.length; i += 2) {
            request = request.queryParam((String) params[i], params[i + 1]);
        }
        return request.get("/api/sheets").then().statusCode(200).extract().jsonPath();
    }

    private static String randomWord() {
        StringBuilder sb = new StringBuilder("zq");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }

    private String createSheet(String title, String composerName, Genre genre) {
        Musician composer = new Musician();
        composer.setName(composerName);
        SheetMusic sheet = new SheetMusic();
        sheet.setTitle(title);
        sheet.setComposer(composer);
        sheet.setGenre(genre);
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
}
