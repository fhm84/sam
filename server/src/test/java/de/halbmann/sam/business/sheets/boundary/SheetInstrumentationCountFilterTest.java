package de.halbmann.sam.business.sheets.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.instruments.CreateInstrument;
import de.halbmann.sam.api.entity.sheets.CreateInstrumentation;
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
 * End-to-end coverage for the {@code instrumentCriterion} search filter (
 * {@code GET /api/sheets?instrumentCriterion=[!]<instrumentId>:<operator>:<count>}), covering the
 * four scenarios from the feature request: an exact count, a zero count, two ANDed exact counts,
 * and an ANDed negated count.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class SheetInstrumentationCountFilterTest {

    private final List<String> createdSheetIds = new ArrayList<>();
    private final List<String> createdInstrumentIds = new ArrayList<>();

    @AfterEach
    void deleteCreatedData() {
        createdSheetIds.forEach(id -> given().delete("/api/sheets/{id}", id));
        createdSheetIds.clear();
        createdInstrumentIds.forEach(id -> given().delete("/api/instruments/{id}", id));
        createdInstrumentIds.clear();
    }

    @Test
    void exactCount_returnsOnlySheetsWithThatManyOfTheInstrument() {
        String horn = createInstrument("Horn");
        String fourHorns = createSheet("Four Horns " + UUID.randomUUID());
        addInstrumentations(fourHorns, horn, 4);
        String threeHorns = createSheet("Three Horns " + UUID.randomUUID());
        addInstrumentations(threeHorns, horn, 3);

        List<String> ids = searchIds(criterion(horn, "EQ", 4));

        assertTrue(ids.contains(fourHorns));
        assertFalse(ids.contains(threeHorns));
    }

    @Test
    void zeroCount_returnsOnlySheetsWithoutTheInstrument() {
        String horn = createInstrument("Horn");
        String withHorns = createSheet("With Horns " + UUID.randomUUID());
        addInstrumentations(withHorns, horn, 2);
        String withoutHorns = createSheet("Without Horns " + UUID.randomUUID());

        List<String> ids = searchIds(criterion(horn, "EQ", 0));

        assertTrue(ids.contains(withoutHorns));
        assertFalse(ids.contains(withHorns));
    }

    @Test
    void twoAndedExactCounts_returnsOnlySheetsMatchingBoth() {
        String horn = createInstrument("Horn");
        String oboe = createInstrument("Oboe");
        String twoAndTwo = createSheet("Two Horns Two Oboes " + UUID.randomUUID());
        addInstrumentations(twoAndTwo, horn, 2);
        addInstrumentations(twoAndTwo, oboe, 2);
        String twoAndThree = createSheet("Two Horns Three Oboes " + UUID.randomUUID());
        addInstrumentations(twoAndThree, horn, 2);
        addInstrumentations(twoAndThree, oboe, 3);

        List<String> ids = searchIds(criterion(horn, "EQ", 2), criterion(oboe, "EQ", 2));

        assertTrue(ids.contains(twoAndTwo));
        assertFalse(ids.contains(twoAndThree));
    }

    @Test
    void negatedCriterion_excludesSheetsMatchingTheNegatedComparison() {
        String horn = createInstrument("Horn");
        String clarinet = createInstrument("Clarinet");
        String oboe = createInstrument("Oboe");
        String withoutOboes = createSheet("Without Oboes " + UUID.randomUUID());
        addInstrumentations(withoutOboes, horn, 4);
        addInstrumentations(withoutOboes, clarinet, 2);
        String withOboe = createSheet("With Oboe " + UUID.randomUUID());
        addInstrumentations(withOboe, horn, 4);
        addInstrumentations(withOboe, clarinet, 2);
        addInstrumentations(withOboe, oboe, 1);

        List<String> ids =
                searchIds(criterion(horn, "EQ", 4), criterion(clarinet, "EQ", 2), "!" + criterion(oboe, "GTE", 1));

        assertTrue(ids.contains(withoutOboes));
        assertFalse(ids.contains(withOboe));
    }

    private List<String> searchIds(String... criteria) {
        var request = given().queryParam("page", 0).queryParam("size", 50);
        for (String criterion : criteria) {
            request = request.queryParam("instrumentCriterion", criterion);
        }
        return request.get("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("data.id", String.class);
    }

    private String criterion(String instrumentId, String operator, int count) {
        return instrumentId + ":" + operator + ":" + count;
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

    private String createInstrument(String name) {
        CreateInstrument instrument = new CreateInstrument();
        String id = (name.toUpperCase(java.util.Locale.ROOT) + "_" + UUID.randomUUID()).replace("-", "_");
        instrument.setId(id);
        instrument.setName(name);
        given().contentType(ContentType.JSON)
                .body(instrument)
                .post("/api/instruments")
                .then()
                .statusCode(200);
        createdInstrumentIds.add(id);
        return id;
    }

    private void addInstrumentations(String sheetId, String instrumentId, int count) {
        for (int i = 1; i <= count; i++) {
            CreateInstrumentation instrumentation = new CreateInstrumentation();
            instrumentation.setInstrumentId(instrumentId);
            instrumentation.setPartLabel(String.valueOf(i));
            given().contentType(ContentType.JSON)
                    .body(instrumentation)
                    .post("/api/sheets/{sheetId}/instrumentations", sheetId)
                    .then()
                    .statusCode(204);
        }
    }
}
