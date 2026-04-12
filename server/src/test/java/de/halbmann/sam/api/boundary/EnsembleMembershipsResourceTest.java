package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import de.halbmann.sam.api.entity.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the ensemble memberships sub-resource.
 *
 * <p>All tests run as {@code music_librarian} (class-level {@code @TestSecurity}) unless
 * overridden per-method. OIDC is disabled in the test profile
 * ({@code %test.quarkus.oidc.enabled=false}); {@code @TestSecurity} provides the mock identity.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class EnsembleMembershipsResourceTest {

    private String ensembleId;
    private String musicianId;

    @BeforeEach
    void setUp() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            // Create an ensemble to work with
            CreateEnsemble createEnsemble = new CreateEnsemble();
            createEnsemble.setName("Test Ensemble");
            ensembleId = given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(createEnsemble))
                    .post("/api/ensembles")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("id");

            // Create a musician to add as member
            Musician musician = new Musician();
            musician.setName("Test Musician");
            musicianId = given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(musician))
                    .post("/api/musicians")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("id");
        }
    }

    @Test
    void listMembers_emptyInitially() {
        given().get("/api/ensembles/{id}/members", ensembleId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(0));
    }

    @Test
    void addMember_returnsCreatedMembership() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            CreateEnsembleMembership membership = new CreateEnsembleMembership();
            membership.setMusicianId(java.util.UUID.fromString(musicianId));
            membership.setConductor(false);

            given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(membership))
                    .post("/api/ensembles/{id}/members", ensembleId)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("id", notNullValue())
                    .body("musician.name", equalTo("Test Musician"))
                    .body("conductor", equalTo(false));
        }
    }

    @Test
    void addMember_thenListReturnsIt() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            CreateEnsembleMembership create = new CreateEnsembleMembership();
            create.setMusicianId(java.util.UUID.fromString(musicianId));
            create.setConductor(true);

            given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(create))
                    .post("/api/ensembles/{id}/members", ensembleId)
                    .then()
                    .statusCode(200);

            given().get("/api/ensembles/{id}/members", ensembleId)
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(1))
                    .body("[0].musician.name", equalTo("Test Musician"))
                    .body("[0].conductor", equalTo(true));
        }
    }

    @Test
    void updateMember_changeConductorFlag() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            CreateEnsembleMembership membership = new CreateEnsembleMembership();
            membership.setMusicianId(java.util.UUID.fromString(musicianId));
            membership.setConductor(false);

            String memberId = given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(membership))
                    .post("/api/ensembles/{id}/members", ensembleId)
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("id");

            Musician musician = new Musician();
            musician.setName("Test Musician");
            EnsembleMembership update = new EnsembleMembership();
            update.setMusician(musician);
            update.setConductor(true);

            given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(update))
                    .put("/api/ensembles/{ensembleId}/members/{memberId}", ensembleId, memberId)
                    .then()
                    .statusCode(200)
                    .body("conductor", equalTo(true));
        }
    }

    @Test
    void deleteMember_removesFromList() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            CreateEnsembleMembership membership = new CreateEnsembleMembership();
            membership.setMusicianId(java.util.UUID.fromString(musicianId));
            membership.setConductor(false);

            String memberId = given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(membership))
                    .post("/api/ensembles/{id}/members", ensembleId)
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("id");

            given().delete("/api/ensembles/{ensembleId}/members/{memberId}", ensembleId, memberId)
                    .then()
                    .statusCode(204);

            given().get("/api/ensembles/{id}/members", ensembleId)
                    .then()
                    .statusCode(200)
                    .body("$", hasSize(0));
        }
    }
}
