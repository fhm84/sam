package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.halbmann.sam.api.entity.ensembles.CoverageResult;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.business.ensembles.boundary.CoverageSnapshotRepository;
import de.halbmann.sam.business.ensembles.boundary.EnsembleRepository;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class SheetsExploreResourceTest {

    @Inject
    SheetRepository sheetRepository;

    @Inject
    EnsembleRepository ensembleRepository;

    @Inject
    CoverageSnapshotRepository coverageSnapshotRepository;

    private final List<String> createdSheetIds = new ArrayList<>();
    private final List<String> createdCollectionIds = new ArrayList<>();
    private final List<String> createdEnsembleIds = new ArrayList<>();

    /**
     * These tests seed sheets/setlists/ensembles through the API. The test database is shared
     * across classes, so leaked rows push other tests' data off default result pages — clean up
     * everything we created. Coverage snapshots cascade on sheet/ensemble deletion.
     */
    @AfterEach
    void deleteCreatedData() {
        createdCollectionIds.forEach(id -> given().delete("/api/sheet-collections/{id}", id));
        createdSheetIds.forEach(id -> given().delete("/api/sheets/{id}", id));
        createdEnsembleIds.forEach(id -> given().delete("/api/ensembles/{id}", id));
        createdCollectionIds.clear();
        createdSheetIds.clear();
        createdEnsembleIds.clear();
    }

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
            String id = given().contentType(ContentType.JSON)
                    .body(json)
                    .post("/api/sheets")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("id");
            createdSheetIds.add(id);
            return id;
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

    private String createSetlist(LocalDate date, String... sheetIds) {
        String collectionId = given().contentType(ContentType.JSON)
                .body(Map.of(
                        "name", "Explore Setlist " + UUID.randomUUID(), "type", "SETLIST", "date", date.toString()))
                .post("/api/sheet-collections")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        createdCollectionIds.add(collectionId);
        for (String sheetId : sheetIds) {
            given().contentType(ContentType.JSON)
                    .body(Map.of("type", "SHEET", "sheetId", sheetId))
                    .post("/api/sheet-collections/{collectionId}/items", collectionId)
                    .then()
                    .statusCode(204);
        }
        return collectionId;
    }

    @Test
    void testExploreShelves_crowdPleasersCountRecentSetlistAppearancesOnly() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString();
        String pulledTitle = "Explore Crowd Pleaser " + uniqueSuffix;
        String stalePullTitle = "Explore Stale Pull " + uniqueSuffix;
        String neverPulledTitle = "Explore Never Pulled " + uniqueSuffix;

        String pulledId = createSheet(pulledTitle, null);
        String stalePullId = createSheet(stalePullTitle, null);
        createSheet(neverPulledTitle, null);

        // Two recent pulls for the crowd pleaser, one pull older than the 12-month window.
        createSetlist(LocalDate.now(), pulledId);
        createSetlist(LocalDate.now().minusMonths(2), pulledId);
        createSetlist(LocalDate.now().minusYears(2), stalePullId);

        given().get("/api/sheets/explore")
                .then()
                .statusCode(200)
                .body("crowdPleasers.title", hasItem(pulledTitle))
                .body("crowdPleasers.title", not(hasItem(stalePullTitle)))
                .body("crowdPleasers.title", not(hasItem(neverPulledTitle)));
    }

    @Test
    void testHiddenGems_excludeAnySheetThatEverAppearedInASetlist() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString();
        String pulledTitle = "Explore Pulled Gem " + uniqueSuffix;
        String neverPulledTitle = "Explore Untouched Gem " + uniqueSuffix;

        String pulledId = createSheet(pulledTitle, null);
        createSheet(neverPulledTitle, null);
        createSetlist(LocalDate.now().minusYears(3), pulledId);

        // The REST shelf is capped, so assert against the repository with an uncapped limit:
        // a setlist appearance at any point in time disqualifies, absence qualifies.
        var hiddenGemTitles = sheetRepository.findHiddenGems(Integer.MAX_VALUE).stream()
                .map(SheetMusicEntity::getTitle)
                .toList();
        assertTrue(hiddenGemTitles.contains(neverPulledTitle));
        assertFalse(hiddenGemTitles.contains(pulledTitle));

        given().get("/api/sheets/explore")
                .then()
                .statusCode(200)
                .body("hiddenGems", notNullValue())
                .body("hiddenGems.title", not(hasItem(pulledTitle)));
    }

    @Test
    void testNeedsAttention_onlyPopulatedForEnsembleWithIncompleteCoverage() throws Exception {
        String sheetTitle = "Explore Needs Attention " + UUID.randomUUID();
        String sheetId = createSheet(sheetTitle, null);

        String ensembleId = given().contentType(ContentType.JSON)
                .body(Map.of("name", "Explore Ensemble " + UUID.randomUUID()))
                .post("/api/ensembles")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        createdEnsembleIds.add(ensembleId);

        // Seed a single INCOMPLETE snapshot directly — computing real coverage would snapshot
        // every sheet in the shared test database and make shelf membership nondeterministic.
        QuarkusTransaction.requiringNew().run(() -> {
            var ensemble = ensembleRepository.findById(UUID.fromString(ensembleId));
            var sheet = sheetRepository.findById(UUID.fromString(sheetId));
            CoverageResult result = new CoverageResult();
            result.setCoverageScore(0.2);
            result.setMissingRequired(true);
            coverageSnapshotRepository.upsert(ensemble, sheet, result, "[]");
        });

        // Without an ensemble the shelf stays empty.
        given().get("/api/sheets/explore").then().statusCode(200).body("needsAttention", empty());

        // With the ensemble the sheet shows up, including its coverage summary.
        given().queryParam("ensemble", ensembleId)
                .get("/api/sheets/explore")
                .then()
                .statusCode(200)
                .body("needsAttention.title", hasItem(sheetTitle))
                .body(
                        "needsAttention.find { it.id == '%s' }.coverage.status".formatted(sheetId),
                        equalTo("INCOMPLETE"));
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
