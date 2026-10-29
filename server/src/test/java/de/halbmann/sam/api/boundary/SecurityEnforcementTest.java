package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;

import de.halbmann.sam.api.entity.ensembles.CreateEnsemble;
import de.halbmann.sam.api.entity.members.Member;
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
    void unauthenticated_postMember_returns401() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            Member member = new Member();
            member.setName("Unauthorized Member");
            given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(member))
                    .post("/api/members")
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
    void authenticated_noRole_postMember_returns403() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            Member member = new Member();
            member.setName("Forbidden Member");
            given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(member))
                    .post("/api/members")
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
    void authenticated_noRole_getMembers_returns200() {
        given().get("/api/members").then().statusCode(200);
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void authenticated_noRole_getPersons_returns200() {
        given().get("/api/persons").then().statusCode(200);
    }

    @Test
    void unauthenticated_getPersons_returns401() {
        given().get("/api/persons").then().statusCode(401);
    }

    // ── Admin-only: member user-link endpoints ───────────────────────────

    @Test
    void unauthenticated_linkMemberUser_returns401() {
        given().put("/api/members/00000000-0000-0000-0000-000000000001/user/some-user-id")
                .then()
                .statusCode(401);
    }

    @Test
    void unauthenticated_unlinkMemberUser_returns401() {
        given().delete("/api/members/00000000-0000-0000-0000-000000000001/user")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void musicLibrarian_linkMemberUser_returns403() {
        given().put("/api/members/00000000-0000-0000-0000-000000000001/user/some-user-id")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void musicLibrarian_unlinkMemberUser_returns403() {
        given().delete("/api/members/00000000-0000-0000-0000-000000000001/user")
                .then()
                .statusCode(403);
    }

    // ── Admin-only: event log ─────────────────────────────────────────────

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void musicLibrarian_getEventLogs_returns403() {
        given().get("/api/event-logs").then().statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "admin1",
            roles = {"admin"})
    void admin_getEventLogs_returns200() {
        given().get("/api/event-logs").then().statusCode(200);
    }

    // ── Admin-only: user search endpoint ──────────────────────────────────

    @Test
    void unauthenticated_adminUserSearch_returns401() {
        given().get("/api/admin/users").then().statusCode(401);
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void musicLibrarian_adminUserSearch_returns403() {
        given().get("/api/admin/users").then().statusCode(403);
    }

    // ── Librarian-only reads: upload inbox and GEMA report ─────────────────

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void authenticated_noRole_getUnlinkedDocuments_returns403() {
        given().get("/api/documents/unlinked").then().statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void musicLibrarian_getUnlinkedDocuments_returns200() {
        given().get("/api/documents/unlinked").then().statusCode(200);
    }

    @Test
    @TestSecurity(
            user = "musician1",
            roles = {})
    void authenticated_noRole_getGemaSetlist_returns403() {
        given().get("/api/sheet-collections/00000000-0000-0000-0000-000000000001/gema-setlist")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(
            user = "librarian1",
            roles = {"music_librarian"})
    void musicLibrarian_getGemaSetlist_unknownCollection_returns404() {
        given().get("/api/sheet-collections/00000000-0000-0000-0000-000000000001/gema-setlist")
                .then()
                .statusCode(404);
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
    void admin_postMember_succeeds() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            Member member = new Member();
            member.setName("Admin Member");
            given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(member))
                    .post("/api/members")
                    .then()
                    .statusCode(200);
        }
    }
}
