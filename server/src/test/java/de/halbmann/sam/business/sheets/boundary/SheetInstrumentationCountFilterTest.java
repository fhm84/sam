package de.halbmann.sam.business.sheets.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.instruments.CreateInstrument;
import de.halbmann.sam.api.entity.sheets.CreateInstrumentation;
import de.halbmann.sam.api.entity.sheets.Genre;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.business.sheets.controller.SheetService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end coverage for the {@code instrumentCriterion} search filter (
 * {@code GET /api/sheets?instrumentCriterion=<instrumentId>:<operator>:<count>}), covering the
 * four scenarios from the feature request (an exact count, a zero count, two ANDed exact counts,
 * and "... but no oboes"), the strict operators, plus combining it with the other filters, full-text search,
 * {@code totalCount}, stable paging and rejection of invalid criteria. Parsing details are covered
 * by {@code InstrumentationCountCriterionTest}.
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
        // "0 horns" alone matches every sheet in the DB; the oboe criterion bounds it to this test's sheets
        String horn = createInstrument("Horn");
        String oboe = createInstrument("Oboe");
        String withHorns = createSheet("With Horns " + UUID.randomUUID());
        addInstrumentations(withHorns, horn, 2);
        addInstrumentations(withHorns, oboe, 1);
        String withoutHorns = createSheet("Without Horns " + UUID.randomUUID());
        addInstrumentations(withoutHorns, oboe, 1);

        List<String> ids = searchIds(criterion(horn, "EQ", 0), criterion(oboe, "EQ", 1));

        assertEquals(List.of(withoutHorns), ids);
    }

    @Test
    void atMostCount_includesSheetsWithFewerOrEqualParts() {
        String horn = createInstrument("Horn");
        String twoHorns = createSheet("Two Horns " + UUID.randomUUID());
        addInstrumentations(twoHorns, horn, 2);
        String threeHorns = createSheet("Three Horns " + UUID.randomUUID());
        addInstrumentations(threeHorns, horn, 3);
        String fourHorns = createSheet("Four Horns " + UUID.randomUUID());
        addInstrumentations(fourHorns, horn, 4);

        List<String> ids = searchIds(criterion(horn, "GTE", 1), criterion(horn, "LTE", 3));

        assertEquals(Set.of(twoHorns, threeHorns), Set.copyOf(ids));
    }

    @Test
    void strictAndNotEqualOperators_excludeTheBoundary() {
        String horn = createInstrument("Horn");
        String oneHorn = createSheet("One Horn " + UUID.randomUUID());
        addInstrumentations(oneHorn, horn, 1);
        String twoHorns = createSheet("Two Horns " + UUID.randomUUID());
        addInstrumentations(twoHorns, horn, 2);
        String threeHorns = createSheet("Three Horns " + UUID.randomUUID());
        addInstrumentations(threeHorns, horn, 3);

        assertEquals(Set.of(twoHorns), Set.copyOf(searchIds(criterion(horn, "GT", 1), criterion(horn, "LT", 3))));
        assertEquals(
                Set.of(oneHorn, threeHorns),
                Set.copyOf(searchIds(criterion(horn, "GTE", 1), criterion(horn, "NEQ", 2))));
    }

    @Test
    void totalCount_countsAllMatchesAcrossPages() {
        String horn = createInstrument("Horn");
        for (int i = 0; i < 3; i++) {
            addInstrumentations(createSheet("Horn Sheet " + i + " " + UUID.randomUUID()), horn, 1);
        }
        addInstrumentations(createSheet("Two Horn Sheet " + UUID.randomUUID()), horn, 2);

        JsonPath firstPage = given().queryParam("page", 0)
                .queryParam("size", 2)
                .queryParam("instrumentCriterion", criterion(horn, "EQ", 1))
                .get("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        assertEquals(2, firstPage.getList("data").size());
        assertEquals(3, firstPage.getLong("totalCount"));
    }

    @Test
    void fullTextSearch_appliesInstrumentCriteria() {
        String token = searchToken();
        String horn = createInstrument("Horn");
        String withHorns = createSheet("Alpha " + token);
        addInstrumentations(withHorns, horn, 2);
        createSheet("Beta " + token);

        JsonPath result = given().queryParam("q", token)
                .queryParam("instrumentCriterion", criterion(horn, "EQ", 2))
                .get("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        assertEquals(List.of(withHorns), result.getList("data.id", String.class));
        assertEquals(1, result.getLong("totalCount"));
    }

    @Test
    void defaultSort_ordersByTitleThenIdSoPagingIsStable() {
        // Equal titles: without the id tiebreaker, LIMIT/OFFSET pages may repeat or skip sheets.
        // Same title needs distinct publishers, otherwise the sheets share a fingerprint.
        String horn = createInstrument("Horn");
        String title = "Same Title " + UUID.randomUUID();
        for (int i = 0; i < 6; i++) {
            addInstrumentations(createSheet(title, null, "Publisher " + i), horn, 1);
        }
        String earlierTitle = createSheet("0 Earlier " + UUID.randomUUID());
        addInstrumentations(earlierTitle, horn, 1);

        List<String> paged = new ArrayList<>();
        for (int page = 0; page < 4; page++) {
            paged.addAll(given().queryParam("page", page)
                    .queryParam("size", 2)
                    .queryParam("instrumentCriterion", criterion(horn, "EQ", 1))
                    .get("/api/sheets")
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getList("data.id", String.class));
        }

        assertEquals(earlierTitle, paged.getFirst());
        List<String> sameTitle = paged.subList(1, paged.size());
        // Postgres orders UUIDs bytewise, i.e. like their lowercase hex strings
        assertEquals(sameTitle.stream().sorted().toList(), sameTitle);
        assertEquals(6, Set.copyOf(sameTitle).size());
    }

    @Test
    void descendingSort_breaksTiesByDescendingId() {
        String horn = createInstrument("Horn");
        String title = "Same Title " + UUID.randomUUID();
        for (int i = 0; i < 4; i++) {
            addInstrumentations(createSheet(title, null, "Publisher " + i), horn, 1);
        }

        List<String> ids = given().queryParam("sortBy", "title")
                .queryParam("sortOrder", "DESC")
                .queryParam("instrumentCriterion", criterion(horn, "EQ", 1))
                .get("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("data.id", String.class);

        assertEquals(ids.stream().sorted(Comparator.reverseOrder()).toList(), ids);
        assertEquals(4, ids.size());
    }

    @Test
    void unknownInstrument_isRejectedOnListAndSearch() {
        String unknown = "NO_SUCH_INSTRUMENT_" + UUID.randomUUID().toString().replace("-", "_") + ":EQ:0";
        given().queryParam("instrumentCriterion", unknown)
                .get("/api/sheets")
                .then()
                .statusCode(400);
        given().queryParam("q", "anything")
                .queryParam("instrumentCriterion", unknown)
                .get("/api/sheets")
                .then()
                .statusCode(400);
    }

    @Test
    void tooManyCriteria_areRejected() {
        String horn = createInstrument("Horn");
        var request = given();
        for (int i = 0; i <= SheetService.MAX_INSTRUMENT_CRITERIA; i++) {
            request = request.queryParam("instrumentCriterion", criterion(horn, "GTE", i));
        }
        request.get("/api/sheets").then().statusCode(400);
    }

    @Test
    void malformedCriterion_isRejectedOnListAndSearch() {
        String horn = createInstrument("Horn");
        given().queryParam("instrumentCriterion", horn + ":EQ:4:")
                .get("/api/sheets")
                .then()
                .statusCode(400);
        given().queryParam("q", "anything")
                .queryParam("instrumentCriterion", "garbage")
                .get("/api/sheets")
                .then()
                .statusCode(400);
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
    void zeroCountAndedWithOthers_excludesSheetsWithTheInstrument() {
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

        List<String> ids = searchIds(criterion(horn, "EQ", 4), criterion(clarinet, "EQ", 2), criterion(oboe, "EQ", 0));

        assertTrue(ids.contains(withoutOboes));
        assertFalse(ids.contains(withOboe));
    }

    @Test
    void genreCombinedWithInstrumentCriterion_filtersByBothWithoutError() {
        String horn = createInstrument("Horn");
        String marchWithHorns = createSheet("March With Horns " + UUID.randomUUID(), Genre.MARCH);
        addInstrumentations(marchWithHorns, horn, 4);
        String waltzWithHorns = createSheet("Waltz With Horns " + UUID.randomUUID(), Genre.WALTZ);
        addInstrumentations(waltzWithHorns, horn, 4);
        String marchWithoutHorns = createSheet("March Without Horns " + UUID.randomUUID(), Genre.MARCH);

        List<String> ids = searchIds(Genre.MARCH, criterion(horn, "EQ", 4));

        assertTrue(ids.contains(marchWithHorns));
        assertFalse(ids.contains(waltzWithHorns));
        assertFalse(ids.contains(marchWithoutHorns));
    }

    private List<String> searchIds(String... criteria) {
        return searchIds(null, criteria);
    }

    private List<String> searchIds(Genre genre, String... criteria) {
        var request = given().queryParam("page", 0).queryParam("size", 50);
        if (genre != null) {
            request = request.queryParam("genre", genre.name());
        }
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

    /** Random alphabetic word, so only this test's sheets match a full-text search for it. */
    private static String searchToken() {
        StringBuilder sb = new StringBuilder("zq");
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }

    private String createSheet(String title) {
        return createSheet(title, null, null);
    }

    private String createSheet(String title, Genre genre) {
        return createSheet(title, genre, null);
    }

    private String createSheet(String title, Genre genre, String publisher) {
        SheetMusic sheet = new SheetMusic();
        sheet.setTitle(title);
        sheet.setGenre(genre);
        sheet.setPublisher(publisher);
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
