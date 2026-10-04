package de.halbmann.sam.business.sheets.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.sheets.Genre;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A full-text search ({@code q}) must honour the same narrowing filters as the plain list
 * (genre, first letter, tag) and report a real {@code totalCount}. Regression test: the search
 * path used to ignore every other filter and never set {@code totalCount}, so the UI paginator
 * collapsed to a single page.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class SheetSearchFilterRegressionTest {

    private final List<String> createdSheetIds = new ArrayList<>();

    /** Random alphabetic word, so only this test's sheets match the search. */
    private String token;

    @BeforeEach
    void newToken() {
        StringBuilder sb = new StringBuilder("zq");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        token = sb.toString();
    }

    @AfterEach
    void deleteCreatedData() {
        createdSheetIds.forEach(id -> given().delete("/api/sheets/{id}", id));
        createdSheetIds.clear();
    }

    @Test
    void search_reportsTotalCountAcrossPages() {
        createSheet("Alpha " + token, Genre.MARCH);
        createSheet("Beta " + token, Genre.MARCH);
        createSheet("Gamma " + token, Genre.WALTZ);

        JsonPath firstPage = search(2);

        assertEquals(2, firstPage.getList("data").size());
        assertEquals(3, firstPage.getLong("totalCount"));
    }

    @Test
    void search_withGenre_returnsOnlyMatchingGenre() {
        String marchId = createSheet("Alpha " + token, Genre.MARCH);
        String waltzId = createSheet("Beta " + token, Genre.WALTZ);

        JsonPath result = search(20, "genre", Genre.MARCH.name());

        assertEquals(List.of(marchId), result.getList("data.id", String.class));
        assertEquals(1, result.getLong("totalCount"));
        assertFalse(result.getList("data.id", String.class).contains(waltzId));
    }

    @Test
    void search_withTitleStartsWith_returnsOnlyMatchingLetter() {
        String alphaId = createSheet("Alpha " + token, Genre.MARCH);
        createSheet("Beta " + token, Genre.MARCH);

        JsonPath result = search(20, "titleStartsWith", "a");

        assertEquals(List.of(alphaId), result.getList("data.id", String.class));
        assertEquals(1, result.getLong("totalCount"));
    }

    @Test
    void search_withTag_returnsOnlyTaggedSheets() {
        String taggedId = createSheet("Alpha " + token, Genre.MARCH);
        createSheet("Beta " + token, Genre.MARCH);
        given().contentType(ContentType.JSON)
                .body(Set.of("christmas-" + token))
                .post("/api/sheets/{id}/tags", taggedId)
                .then()
                .statusCode(204);

        JsonPath result = search(20, "tag", "christmas-" + token);

        assertEquals(List.of(taggedId), result.getList("data.id", String.class));
        assertEquals(1, result.getLong("totalCount"));
    }

    /** Searches for {@link #token} on page 0; {@code filters} are name/value pairs. */
    private JsonPath search(int size, Object... filters) {
        var request = given().queryParam("q", token).queryParam("page", 0).queryParam("size", size);
        for (int i = 0; i < filters.length; i += 2) {
            request = request.queryParam((String) filters[i], filters[i + 1]);
        }
        return request.get("/api/sheets").then().statusCode(200).extract().jsonPath();
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
