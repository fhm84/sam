package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.halbmann.sam.api.entity.musicians.Musician;
import de.halbmann.sam.api.entity.sheets.DifficultyLevel;
import de.halbmann.sam.api.entity.sheets.Genre;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.api.entity.sheets.Style;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class SheetsResourceTest {

    @Test
    void testAddingSheetMusic() throws Exception {
        final SheetMusic sheet = new SheetMusic();
        sheet.setTitle("My first sheet");
        sheet.setGenre(Genre.SYMPHONY);
        sheet.setStyle(Style.CLASSICAL);
        sheet.setDifficultyLevel(DifficultyLevel.EASY);
        sheet.setPublisher("Who knows");
        sheet.setYearOfComposition(1875);
        final Musician musician = new Musician();
        musician.setName("This crazy guy");
        sheet.setComposer(musician);

        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(sheet))
                    .post("/api/sheets")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("title", equalTo(sheet.getTitle()))
                    .body("genre", equalTo(sheet.getGenre().name()))
                    .body("difficultyLevel", equalTo(sheet.getDifficultyLevel().getGrade()))
                    .body("publisher", equalTo(sheet.getPublisher()))
                    .body("yearOfComposition", equalTo(sheet.getYearOfComposition()))
                    .body("composer.name", equalTo(musician.getName()));
        }
    }

    @Test
    void testAddingSheetMusic_plainJson() {
        final String testJson = """
                        {
                            "title": "Kuschelpolka",
                            "publisher": "Ewoton Verlag",
                            "composer": {
                                "name": "Frank Bernaerts"
                            },
                            "genre": "POLKA",
                            "type":""
                        }
                        """;

        SheetMusic sheetResponse = given().contentType(ContentType.JSON)
                .body(testJson)
                .post("/api/sheets")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .body()
                .as(SheetMusic.class);

        assertNotNull(sheetResponse);
        assertEquals("Kuschelpolka", sheetResponse.getTitle());
        assertEquals(Genre.POLKA, sheetResponse.getGenre());
        assertEquals("Ewoton Verlag", sheetResponse.getPublisher());
        assertNotNull(sheetResponse.getComposer());
        assertEquals("Frank Bernaerts", sheetResponse.getComposer().getName());

        // Filter by title: the shared test database may hold sheets from other test classes,
        // so the unfiltered first page is not guaranteed to contain the new sheet.
        given().queryParam("title", "Kuschelpolka")
                .get("/api/sheets")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size", greaterThanOrEqualTo(1))
                .body("totalCount", greaterThanOrEqualTo(1))
                .body("data.title", hasItem("Kuschelpolka"));
    }

    @Test
    void testCreateSheetWizardFields() {
        // collection to link at create time
        final String collectionId = given().contentType(ContentType.JSON)
                .body("""
                        {"name": "Wizard Test Collection", "type": "FOLDER"}
                        """)
                .post("/api/sheet-collections")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        // sheet with source and collection link
        final String sheetId = given().contentType(ContentType.JSON)
                .body("""
                        {
                            "title": "Wizard Piece",
                            "genre": "POLKA",
                            "source": "Musikverlag Test",
                            "collectionId": "%s"
                        }
                        """.formatted(collectionId))
                .post("/api/sheets")
                .then()
                .statusCode(200)
                .body("source", equalTo("Musikverlag Test"))
                .extract()
                .path("id");

        // the new sheet was added to the collection
        given().get("/api/sheet-collections/{collectionId}/items", collectionId)
                .then()
                .statusCode(200)
                .body("data.sheetId", hasItem(sheetId));

        // instrumentation with pages
        given().contentType(ContentType.JSON)
                .body("""
                        {"id": "TEST_WIZARD_FLUTE", "name": "Wizard Flute", "transposition": "C"}
                        """)
                .post("/api/instruments")
                .then()
                .statusCode(200);

        given().contentType(ContentType.JSON)
                .body("""
                        {"instrumentId": "TEST_WIZARD_FLUTE", "partLabel": "1", "pages": "1-2"}
                        """)
                .post("/api/sheets/{sheetId}/instrumentations", sheetId)
                .then()
                .statusCode(204);

        given().get("/api/sheets/{sheetId}/instrumentations", sheetId)
                .then()
                .statusCode(200)
                .body("pages", hasItem("1-2"));

        // deleting a sheet that is still in a collection must remove the membership, not fail
        given().delete("/api/sheets/{sheetId}", sheetId).then().statusCode(204);

        given().get("/api/sheet-collections/{collectionId}/items", collectionId)
                .then()
                .statusCode(200)
                .body("data.sheetId", not(hasItem(sheetId)));
    }

    @Test
    void testRightsStatusAndGemaPflichtig_roundTripOnCreateAndUpdate() {
        final String sheetId = given().contentType(ContentType.JSON)
                .body("""
                        {
                            "title": "Rights Test Piece",
                            "genre": "POLKA",
                            "rightsStatus": "PERMITTED_ARCHIVE",
                            "gemaPflichtig": "YES"
                        }
                        """)
                .post("/api/sheets")
                .then()
                .statusCode(200)
                .body("rightsStatus", equalTo("PERMITTED_ARCHIVE"))
                .body("gemaPflichtig", equalTo("YES"))
                .extract()
                .path("id");

        given().get("/api/sheets/{sheetId}", sheetId)
                .then()
                .statusCode(200)
                .body("rightsStatus", equalTo("PERMITTED_ARCHIVE"))
                .body("gemaPflichtig", equalTo("YES"));

        given().contentType(ContentType.JSON)
                .body("""
                        {
                            "title": "Rights Test Piece",
                            "genre": "POLKA",
                            "rightsStatus": "NO_DIGITALIZATION",
                            "gemaPflichtig": "NO"
                        }
                        """)
                .put("/api/sheets/{sheetId}", sheetId)
                .then()
                .statusCode(204);

        given().get("/api/sheets/{sheetId}", sheetId)
                .then()
                .statusCode(200)
                .body("rightsStatus", equalTo("NO_DIGITALIZATION"))
                .body("gemaPflichtig", equalTo("NO"));
    }
}
