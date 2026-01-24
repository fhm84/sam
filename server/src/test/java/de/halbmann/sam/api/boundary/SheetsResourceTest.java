package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import de.halbmann.sam.api.entity.Musician;
import de.halbmann.sam.api.entity.SheetMusic;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SheetsResourceTest {

  @Test
  void testAddingSheetMusic() throws Exception {
    final SheetMusic sheet = new SheetMusic();
    sheet.setTitle("My first sheet");
    sheet.setGenre("Classical");
    sheet.setDifficultyLevel("Beginner");
    sheet.setPublisher("Who knows");
    sheet.setYearOfComposition(1875);
    final Musician musician = new Musician();
    musician.setName("This crazy guy");
    sheet.setComposer(musician);

    try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
      given()
          .contentType(ContentType.JSON)
          .body(jsonb.toJson(sheet))
          .post("/api/sheets")
          .then()
          .statusCode(200)
          .contentType(ContentType.JSON)
          .body("title", equalTo(sheet.getTitle()))
          .body("genre", equalTo(sheet.getGenre()))
          .body("difficultyLevel", equalTo(sheet.getDifficultyLevel()))
          .body("publisher", equalTo(sheet.getPublisher()))
          .body("yearOfComposition", equalTo(sheet.getYearOfComposition()))
          .body("composer.name", equalTo(musician.getName()));
    }
  }

  @Test
  void testAddingSheetMusic_plainJson() {
    final String testJson =
        """
                {
                    "title": "Kuschelpolka",
                    "publisher": "Ewoton Verlag",
                    "composer": {
                        "name": "Frank Bernaerts"
                    },
                    "genre": "Polka",
                    "type":""
                }
                """;

    SheetMusic sheetResponse =
        given()
            .contentType(ContentType.JSON)
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
    assertEquals("Polka", sheetResponse.getGenre());
    assertEquals("Ewoton Verlag", sheetResponse.getPublisher());
    assertEquals("Frank Bernaerts", sheetResponse.getComposer().getName());

    given()
        .get("/api/sheets")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("size", greaterThanOrEqualTo(1))
        .body("totalCount", greaterThanOrEqualTo(1))
        .body("data.title", hasItem("Kuschelpolka"));
  }
}
