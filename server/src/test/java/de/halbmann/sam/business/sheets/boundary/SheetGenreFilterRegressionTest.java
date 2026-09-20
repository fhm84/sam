package de.halbmann.sam.business.sheets.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.sheets.Genre;
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
 * {@code genre} filters ({@code GET /api/sheets/letters?genre=...} and {@code GET
 * /api/sheets?genre=...}) must compare against the {@link Genre} enum directly, not its raw
 * String name — {@code SheetMusicEntity.genre} is a basic-valued enum column, not an
 * association, and Hibernate rejects a String bound where a Genre is expected. Regression test
 * for two related bugs: {@code s.genre.name = :genre} (invalid attribute path) and binding the
 * raw filter String instead of {@code Genre.valueOf(...)} (type mismatch).
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class SheetGenreFilterRegressionTest {

    private final List<String> createdSheetIds = new ArrayList<>();

    @AfterEach
    void deleteCreatedData() {
        createdSheetIds.forEach(id -> given().delete("/api/sheets/{id}", id));
        createdSheetIds.clear();
    }

    @Test
    void lettersEndpoint_genreFilter_returnsOnlyLettersFromMatchingGenre() {
        String suffix = UUID.randomUUID().toString();
        createSheet("Zeta " + suffix, Genre.MARCH);
        createSheet("Yankee " + suffix, Genre.WALTZ);

        List<String> marchLetters = given().queryParam("genre", Genre.MARCH.name())
                .get("/api/sheets/letters")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList(".", String.class);

        assertTrue(marchLetters.contains("Z"));
        assertFalse(marchLetters.contains("Y"));
    }

    @Test
    void listEndpoint_genreFilter_returnsOnlyMatchingGenre() {
        String suffix = UUID.randomUUID().toString();
        String marchId = createSheet("March Sheet " + suffix, Genre.MARCH);
        String waltzId = createSheet("Waltz Sheet " + suffix, Genre.WALTZ);

        List<String> ids = given().queryParam("genre", Genre.MARCH.name())
                .queryParam("page", 0)
                .queryParam("size", 20)
                .get("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("data.id", String.class);

        assertTrue(ids.contains(marchId));
        assertFalse(ids.contains(waltzId));
    }

    private String createSheet(String title, Genre genre) {
        SheetMusic sheet = new SheetMusic();
        sheet.setTitle(title);
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
