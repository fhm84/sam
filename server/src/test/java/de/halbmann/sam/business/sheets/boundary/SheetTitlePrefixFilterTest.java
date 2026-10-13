package de.halbmann.sam.business.sheets.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.sheets.SheetMusic;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code titleStartsWith} is a literal prefix. Regression test: it went into {@code LIKE}
 * unescaped, so {@code _} or {@code %} acted as wildcards and matched every title.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class SheetTitlePrefixFilterTest {

    private final List<String> createdSheetIds = new ArrayList<>();

    /** Random alphabetic word, so a full-text search for it only finds this test's sheets. */
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
    void list_treatsWildcardCharactersLiterally() {
        String underscore = createSheet("_Underscore " + token);
        String percent = createSheet("%Percent " + token);
        String plain = createSheet("Plain " + token);

        List<String> underscoreHits = listIds("_");
        assertTrue(underscoreHits.contains(underscore));
        assertFalse(underscoreHits.contains(percent));
        assertFalse(underscoreHits.contains(plain));

        List<String> percentHits = listIds("%");
        assertTrue(percentHits.contains(percent));
        assertFalse(percentHits.contains(underscore));
        assertFalse(percentHits.contains(plain));
    }

    @Test
    void search_treatsWildcardCharactersLiterally() {
        String underscore = createSheet("_Underscore " + token);
        createSheet("Plain " + token);

        List<String> ids = given().queryParam("q", token)
                .queryParam("titleStartsWith", "_")
                .get("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("data.id", String.class);

        assertEquals(List.of(underscore), ids);
    }

    private List<String> listIds(String titleStartsWith) {
        return given().queryParam("titleStartsWith", titleStartsWith)
                .queryParam("size", -1)
                .get("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("data.id", String.class);
    }

    private String createSheet(String title) {
        SheetMusic sheet = new SheetMusic();
        sheet.setTitle(title);
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
