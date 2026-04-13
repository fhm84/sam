package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;

import de.halbmann.sam.api.entity.ensembles.CreateEnsemble;
import de.halbmann.sam.api.entity.musicians.Musician;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the security layer correctly enforces authentication and role requirements on
 * write endpoints. OIDC is disabled in the test profile; {@code @TestSecurity} provides mock
 * identities. Unauthenticated requests (no {@code @TestSecurity}) are handled by the default
 * anonymous identity, which should be rejected by {@code @Authenticated}.
 */
@QuarkusTest
class SecurityEnforcementTest {

    // ── Unauthenticated requests must receive 401 ──────────────────────────

    @Test
    void unauthenticated_getSheets_returns401() {
        given().get("/api/sheets").then().statusCode(401);
    }

    @Test
    void unauthenticated_postSheet_returns401() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            given().contentType(ContentType.JSON)
                    .body("{\"title\":\"Test\"}")
                    .post("/api/sheets")
                    .then()
                    .statusCode(401);
        }
    }

    @Test
    void unauthenticated_postEnsemble_returns401() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            CreateEnsemble ensemble = new CreateEnsemble();
            ensemble.setName("Unauthorized Ensemble");
            given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(ensemble))
                    .post("/api/ensembles")
                    .then()
                    .statusCode(401);
        }
    }

    @Test
    void unauthenticated_postMusician_returns401() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            Musician musician = new Musician();
            musician.setName("Unauthorized Musician");
            given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(musician))
                    .post("/api/musicians")
                    .then()
                    .statusCode(401);
        }
    }

    // ── Authenticated but unprivileged user must receive 403 on writes ─────

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void authenticated_noRole_postEnsemble_returns403() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            CreateEnsemble ensemble = new CreateEnsemble();
            ensemble.setName("Forbidden Ensemble");
            given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(ensemble))
                    .post("/api/ensembles")
                    .then()
                    .statusCode(403);
        }
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void authenticated_noRole_postMusician_returns403() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            Musician musician = new Musician();
            musician.setName("Forbidden Musician");
            given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(musician))
                    .post("/api/musicians")
                    .then()
                    .statusCode(403);
        }
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void authenticated_noRole_getEnsembles_returns200() {
        // Read operations must succeed for any authenticated user
        given().get("/api/ensembles").then().statusCode(200);
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void authenticated_noRole_getMusicians_returns200() {
        given().get("/api/musicians").then().statusCode(200);
    }

    // ── music_librarian has write access ──────────────────────────────────

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void musicLibrarian_postEnsemble_succeeds() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            CreateEnsemble ensemble = new CreateEnsemble();
            ensemble.setName("Librarian Ensemble");
            given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(ensemble))
                    .post("/api/ensembles")
                    .then()
                    .statusCode(200);
        }
    }

    @Test
    @TestSecurity(
            user = "admin1",
            roles = {"admin"})
    void admin_postMusician_succeeds() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            Musician musician = new Musician();
            musician.setName("Admin Musician");
            given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(musician))
                    .post("/api/musicians")
                    .then()
                    .statusCode(200);
        }
    }
}
