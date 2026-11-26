package de.halbmann.sam.business.sheets.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.halbmann.sam.api.entity.instruments.CreateInstrument;
import de.halbmann.sam.api.entity.sheets.CreateInstrumentation;
import de.halbmann.sam.api.entity.sheets.Genre;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.business.sheets.controller.SheetService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code GET /api/sheets?partCount=<instrumentId>:<comparison>:<parts>} on the list and the full-text
 * search path. Every test creates its own instruments, so only its own sheets can have parts for
 * them; "no X" filters are paired with a positive one for the same reason. Parsing is covered by
 * {@code PartCountFilterTest}.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class SheetPartCountFilterTest {

    /** Undo steps, run last-in first-out: sheets go before the instruments their parts use. */
    private final Deque<Runnable> cleanup = new ArrayDeque<>();

    private String horn;
    private String oboe;
    private String clarinet;

    @BeforeEach
    void createInstruments() {
        horn = instrument("Horn");
        oboe = instrument("Oboe");
        clarinet = instrument("Clarinet");
    }

    @AfterEach
    void removeTestData() {
        while (!cleanup.isEmpty()) {
            cleanup.pop().run();
        }
    }

    @Test
    void eq_matchesExactlyThatManyParts() {
        sheet("3 horns", parts(horn, 3));
        String four = sheet("4 horns", parts(horn, 4));
        sheet("5 horns", parts(horn, 5));

        assertEquals(Set.of(four), matching(horn + ":EQ:4"));
    }

    @Test
    void eqZero_matchesSheetsWithoutTheInstrument() {
        String oboeOnly = sheet("oboe only", parts(oboe, 1));
        sheet("oboe and horns", parts(oboe, 1, horn, 2));

        assertEquals(Set.of(oboeOnly), matching(oboe + ":GTE:1", horn + ":EQ:0"));
    }

    @Test
    void boundsOnTheSameInstrument_selectARange() {
        String one = sheet("1 horn", parts(horn, 1));
        String two = sheet("2 horns", parts(horn, 2));
        String three = sheet("3 horns", parts(horn, 3));
        String four = sheet("4 horns", parts(horn, 4));

        assertEquals(Set.of(two, three), matching(horn + ":GTE:2", horn + ":LTE:3"));
        assertEquals(Set.of(two), matching(horn + ":GT:1", horn + ":LT:3"));
        assertEquals(Set.of(one, three, four), matching(horn + ":GT:0", horn + ":NEQ:2"));
    }

    @Test
    void filtersForDifferentInstruments_mustAllMatch() {
        String wanted = sheet("4 horns, 2 clarinets", parts(horn, 4, clarinet, 2));
        sheet("4 horns, 2 clarinets, oboe", parts(horn, 4, clarinet, 2, oboe, 1));
        sheet("4 horns, 3 clarinets", parts(horn, 4, clarinet, 3));
        sheet("2 clarinets", parts(clarinet, 2));

        assertEquals(Set.of(wanted), matching(horn + ":EQ:4", clarinet + ":EQ:2", oboe + ":EQ:0"));
    }

    @Test
    void combinesWithGenre() {
        String march = sheet("march", Genre.MARCH, null, parts(horn, 2));
        sheet("waltz", Genre.WALTZ, null, parts(horn, 2));
        sheet("march, 1 horn", Genre.MARCH, null, parts(horn, 1));

        Set<String> found = Set.copyOf(listRequest(horn + ":EQ:2")
                .queryParam("genre", Genre.MARCH.name())
                .get("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("data.id", String.class));

        assertEquals(Set.of(march), found);
    }

    @Test
    void narrowsFullTextSearch_includingItsTotal() {
        // A made-up word, so the search finds nothing but this test's sheets
        String word = "qx" + UUID.randomUUID().toString().replaceAll("[^a-f]", "");
        String withOboe = sheet("Serenade " + word, parts(oboe, 1));
        sheet("Nocturne " + word, parts(horn, 1));

        JsonPath page = listRequest(oboe + ":EQ:1")
                .queryParam("q", word)
                .get("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        assertEquals(List.of(withOboe), page.getList("data.id", String.class));
        assertEquals(1, page.getLong("totalCount"));
    }

    @Test
    void totalCount_coversAllPages() {
        for (int i = 0; i < 5; i++) {
            sheet("clarinet piece " + i, parts(clarinet, 1));
        }

        JsonPath page = given().queryParam("partCount", clarinet + ":GTE:1")
                .queryParam("page", 1)
                .queryParam("size", 2)
                .get("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        assertEquals(2, page.getList("data").size());
        assertEquals(5, page.getLong("totalCount"));
    }

    @Test
    void equalTitles_pageInIdOrderWithoutGapsOrRepeats() {
        // Equal titles need different publishers, otherwise the sheets share a fingerprint
        String title = "Same " + UUID.randomUUID();
        String first = sheet("0 " + title, parts(horn, 1));
        for (int i = 0; i < 5; i++) {
            sheet(title, null, "Publisher " + i, parts(horn, 1));
        }

        List<String> seen = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            seen.addAll(given().queryParam("partCount", horn + ":EQ:1")
                    .queryParam("page", page)
                    .queryParam("size", 2)
                    .get("/api/sheets")
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getList("data.id", String.class));
        }

        assertEquals(6, seen.size());
        assertEquals(first, seen.getFirst());
        List<String> sameTitle = seen.subList(1, seen.size());
        // PostgreSQL compares UUIDs byte by byte, which is the order of their lowercase hex form
        assertEquals(sameTitle.stream().sorted().distinct().toList(), sameTitle);
    }

    @Test
    void descendingSort_alsoSortsTiesDescending() {
        String title = "Same " + UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            sheet(title, null, "Publisher " + i, parts(oboe, 2));
        }

        List<String> ids = listRequest(oboe + ":EQ:2")
                .queryParam("sortBy", "title")
                .queryParam("sortOrder", "DESC")
                .get("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("data.id", String.class);

        assertEquals(3, ids.size());
        assertEquals(ids.stream().sorted(Comparator.reverseOrder()).toList(), ids);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void invalidFilters_areRejected(boolean fullTextSearch) {
        String unknownInstrument =
                "NOT_AN_INSTRUMENT_" + UUID.randomUUID().toString().replace('-', '_');
        List<List<String>> invalid = List.of(
                List.of(unknownInstrument + ":EQ:0"),
                List.of(horn + ":EQ:2:"),
                List.of("nonsense"),
                IntStream.rangeClosed(0, SheetService.MAX_PART_COUNT_FILTERS)
                        .mapToObj(n -> horn + ":NEQ:" + n)
                        .toList());

        for (List<String> filters : invalid) {
            RequestSpecification request = given().queryParam("partCount", filters);
            if (fullTextSearch) {
                request.queryParam("q", "march");
            }
            request.get("/api/sheets").then().statusCode(400);
        }
    }

    // ── helpers ──────────────────────────────────────────

    private static Map<String, Integer> parts(Object... instrumentThenCount) {
        Map<String, Integer> parts = new LinkedHashMap<>();
        for (int i = 0; i < instrumentThenCount.length; i += 2) {
            parts.put((String) instrumentThenCount[i], (Integer) instrumentThenCount[i + 1]);
        }
        return parts;
    }

    private Set<String> matching(String... filters) {
        return Set.copyOf(listRequest(filters)
                .get("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("data.id", String.class));
    }

    private static RequestSpecification listRequest(String... filters) {
        return given().queryParam("size", 100).queryParam("partCount", List.of(filters));
    }

    private String sheet(String title, Map<String, Integer> parts) {
        return sheet(title, null, null, parts);
    }

    private String sheet(String title, Genre genre, String publisher, Map<String, Integer> parts) {
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
        cleanup.push(() -> given().delete("/api/sheets/{id}", id));

        parts.forEach((instrument, count) -> {
            for (int part = 1; part <= count; part++) {
                CreateInstrumentation instrumentation = new CreateInstrumentation();
                instrumentation.setInstrumentId(instrument);
                instrumentation.setPartLabel(part + ".");
                given().contentType(ContentType.JSON)
                        .body(instrumentation)
                        .post("/api/sheets/{id}/instrumentations", id)
                        .then()
                        .statusCode(204);
            }
        });
        return id;
    }

    private String instrument(String name) {
        String id = (name + "_" + UUID.randomUUID()).replace('-', '_').toUpperCase(Locale.ROOT);
        CreateInstrument instrument = new CreateInstrument();
        instrument.setId(id);
        instrument.setName(name);
        given().contentType(ContentType.JSON)
                .body(instrument)
                .post("/api/instruments")
                .then()
                .statusCode(200);
        cleanup.push(() -> given().delete("/api/instruments/{id}", id));
        return id;
    }
}
