package de.halbmann.sam.business.sheets.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.ensembles.CoverageResult;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.business.ensembles.boundary.CoverageSnapshotRepository;
import de.halbmann.sam.business.ensembles.boundary.EnsembleRepository;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Executes {@link SheetRepository#findAiAssistantCandidates} against the real database — the
 * query is built dynamically, so only an actual round-trip validates the JPQL (coverage join,
 * tag join, limit).
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class SheetRepositoryAiCandidatesTest {

    @Inject
    SheetRepository sheetRepository;

    @Inject
    EnsembleRepository ensembleRepository;

    @Inject
    CoverageSnapshotRepository coverageSnapshotRepository;

    private final List<String> createdSheetIds = new ArrayList<>();
    private final List<String> createdEnsembleIds = new ArrayList<>();

    @AfterEach
    void deleteCreatedData() {
        createdSheetIds.forEach(id -> given().delete("/api/sheets/{id}", id));
        createdEnsembleIds.forEach(id -> given().delete("/api/ensembles/{id}", id));
        createdSheetIds.clear();
        createdEnsembleIds.clear();
    }

    @Test
    void returnsOnlyPlayableSheets_forTheGivenEnsemble_upToTheLimit() {
        String suffix = UUID.randomUUID().toString();
        String completeId = createSheet("AI Candidates Complete " + suffix);
        String playableId = createSheet("AI Candidates Playable " + suffix);
        String incompleteId = createSheet("AI Candidates Incomplete " + suffix);
        String ensembleId = createEnsemble();
        seedSnapshot(ensembleId, completeId, 1.0, false);
        seedSnapshot(ensembleId, playableId, 0.8, false);
        seedSnapshot(ensembleId, incompleteId, 0.2, true);

        List<UUID> found = findCandidateIds(ensembleId, Set.of(), 20);

        assertTrue(found.contains(UUID.fromString(completeId)));
        assertTrue(found.contains(UUID.fromString(playableId)));
        assertFalse(found.contains(UUID.fromString(incompleteId)));

        List<UUID> limited = findCandidateIds(ensembleId, Set.of(), 1);
        assertEquals(1, limited.size());
    }

    @Test
    void tagFilter_matchesAnyGivenTag_andExcludesUntaggedSheets() {
        String suffix = UUID.randomUUID().toString();
        String tag = "ai-candidates-" + suffix;
        String taggedId = createSheet("AI Candidates Tagged " + suffix);
        String untaggedId = createSheet("AI Candidates Untagged " + suffix);
        given().contentType(ContentType.JSON)
                .body(Set.of(tag))
                .post("/api/sheets/{sheetId}/tags", taggedId)
                .then()
                .statusCode(204);
        String ensembleId = createEnsemble();
        seedSnapshot(ensembleId, taggedId, 1.0, false);
        seedSnapshot(ensembleId, untaggedId, 1.0, false);

        List<UUID> found = findCandidateIds(ensembleId, Set.of(tag, "some-other-tag-" + suffix), 20);

        assertTrue(found.contains(UUID.fromString(taggedId)));
        assertFalse(found.contains(UUID.fromString(untaggedId)));
    }

    private List<UUID> findCandidateIds(String ensembleId, Set<String> tags, int limit) {
        List<UUID> ids = new ArrayList<>();
        QuarkusTransaction.requiringNew()
                .run(() -> sheetRepository
                        .findAiAssistantCandidates(UUID.fromString(ensembleId), null, null, tags, limit)
                        .forEach(s -> ids.add(s.getId())));
        return ids;
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

    private String createEnsemble() {
        String id = given().contentType(ContentType.JSON)
                .body(Map.of("name", "AI Candidates Ensemble " + UUID.randomUUID()))
                .post("/api/ensembles")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        createdEnsembleIds.add(id);
        return id;
    }

    private void seedSnapshot(String ensembleId, String sheetId, double score, boolean missingRequired) {
        QuarkusTransaction.requiringNew().run(() -> {
            var ensemble = ensembleRepository.findById(UUID.fromString(ensembleId));
            SheetMusicEntity sheet = sheetRepository.findById(UUID.fromString(sheetId));
            CoverageResult result = new CoverageResult();
            result.setCoverageScore(score);
            result.setMissingRequired(missingRequired);
            coverageSnapshotRepository.upsert(ensemble, sheet, result, "[]");
        });
    }
}
