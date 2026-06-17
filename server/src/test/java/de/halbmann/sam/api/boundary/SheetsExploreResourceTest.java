package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import de.halbmann.sam.api.entity.sheets.SheetMusic;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class SheetsExploreResourceTest {

    private String createSheet(String title, Duration duration) throws Exception {
        SheetMusic sheet = new SheetMusic();
        sheet.setTitle(title);
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            String json = jsonb.toJson(sheet);
            if (duration != null) {
                // Splice in the duration field manually since SheetMusic has no public setter
                // exposed via a fluent builder in this DTO.
                json = json.substring(0, json.length() - 1) + ",\"duration\":\"" + duration + "\"}";
            }
            return given().contentType(ContentType.JSON)
                    .body(json)
                    .post("/api/sheets")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("id");
        }
    }

    @Test
    void testExploreShelves_groupsSheetsByDurationAndRecency() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString();
        String quickFillerTitle = "Explore Quick Filler " + uniqueSuffix;
        String bigFinishTitle = "Explore Big Finish " + uniqueSuffix;
        String midLengthTitle = "Explore Mid Length " + uniqueSuffix;

        createSheet(quickFillerTitle, Duration.ofMinutes(2));
        createSheet(bigFinishTitle, Duration.ofMinutes(6));
        createSheet(midLengthTitle, Duration.ofMinutes(4));

        given().get("/api/sheets/explore")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("quickFillers.title", hasItem(quickFillerTitle))
                .body("quickFillers.title", not(hasItem(bigFinishTitle)))
                .body("quickFillers.title", not(hasItem(midLengthTitle)))
                .body("bigFinishes.title", hasItem(bigFinishTitle))
                .body("bigFinishes.title", not(hasItem(quickFillerTitle)))
                .body("bigFinishes.title", not(hasItem(midLengthTitle)))
                .body("recentlyAdded.title", hasItems(quickFillerTitle, bigFinishTitle, midLengthTitle))
                .body("tagCloud", notNullValue());
    }

    @Test
    void testExploreSurprisePick_returnsASheet() throws Exception {
        createSheet("Explore Surprise Seed " + UUID.randomUUID(), null);

        // The shared test database always has at least this sheet, so the 204 "empty archive"
        // path is not reachable/exercised here.
        given().get("/api/sheets/explore/surprise")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("title", notNullValue());
    }

    @Test
    void testTagFilter_returnsOnlySheetsWithThatTag() throws Exception {
        String tag = "explore-tag-" + UUID.randomUUID();
        String taggedTitle = "Explore Tagged Sheet " + UUID.randomUUID();
        String untaggedTitle = "Explore Untagged Sheet " + UUID.randomUUID();

        String taggedId = createSheet(taggedTitle, null);
        createSheet(untaggedTitle, null);

        given().contentType(ContentType.JSON)
                .body(Set.of(tag))
                .post("/api/sheets/{sheetId}/tags", taggedId)
                .then()
                .statusCode(204);

        given().queryParam("tag", tag)
                .get("/api/sheets")
                .then()
                .statusCode(200)
                .body("totalCount", equalTo(1))
                .body("data[0].title", equalTo(taggedTitle));
    }
}
