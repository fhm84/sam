package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import de.halbmann.sam.api.entity.ensembles.CreateEnsemble;
import de.halbmann.sam.api.entity.ensembles.CreateEnsembleMembership;
import de.halbmann.sam.api.entity.ensembles.CreateEnsembleVoice;
import de.halbmann.sam.api.entity.ensembles.EnsembleMembership;
import de.halbmann.sam.api.entity.ensembles.EnsembleRole;
import de.halbmann.sam.api.entity.members.Member;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
    private String memberId;

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

            // Create a member to give seats
            Member member = new Member();
            member.setName("Test Member");
            memberId = given().contentType(ContentType.JSON)
                    .body(jsonb.toJson(member))
                    .post("/api/members")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("id");
        }
    }

    @AfterEach
    void tearDown() {
        // Deleting the member removes its seats, so the ensemble can go too
        given().delete("/api/members/{id}", memberId).then().statusCode(204);
        given().delete("/api/ensembles/{id}", ensembleId);
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
    void addMember_withoutRole_isRegularMember() throws Exception {
        given().contentType(ContentType.JSON)
                .body(toJson(seat(null, null)))
                .post("/api/ensembles/{id}/members", ensembleId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", notNullValue())
                .body("member.name", equalTo("Test Member"))
                .body("role", equalTo("MEMBER"));
    }

    @Test
    void addMember_thenListReturnsIt() throws Exception {
        given().contentType(ContentType.JSON)
                .body(toJson(seat(null, EnsembleRole.CONDUCTOR)))
                .post("/api/ensembles/{id}/members", ensembleId)
                .then()
                .statusCode(200);

        given().get("/api/ensembles/{id}/members", ensembleId)
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].member.name", equalTo("Test Member"))
                .body("[0].role", equalTo("CONDUCTOR"));
    }

    @Test
    void updateMember_changesRole() throws Exception {
        String seatId = addSeat(seat(null, EnsembleRole.MEMBER));

        EnsembleMembership update = new EnsembleMembership();
        update.setRole(EnsembleRole.GUEST);

        given().contentType(ContentType.JSON)
                .body(toJson(update))
                .put("/api/ensembles/{ensembleId}/members/{seatId}", ensembleId, seatId)
                .then()
                .statusCode(200)
                .body("role", equalTo("GUEST"));
    }

    @Test
    void updateRole_ofOneVoiceSeat_appliesToTheMembersOtherVoiceSeats() throws Exception {
        String firstSeat = addSeat(seat(addVoice("1. Trumpet"), EnsembleRole.MEMBER));
        addSeat(seat(addVoice("2. Trumpet"), EnsembleRole.MEMBER));
        // The seat without a voice keeps its own role
        addSeat(seat(null, EnsembleRole.CONDUCTOR));

        EnsembleMembership update = new EnsembleMembership();
        update.setVoiceId(UUID.fromString(given().get("/api/ensembles/{id}/members", ensembleId)
                .then()
                .extract()
                .path("find { it.id == '" + firstSeat + "' }.voiceId")));
        update.setRole(EnsembleRole.SUBSTITUTE);
        given().contentType(ContentType.JSON)
                .body(toJson(update))
                .put("/api/ensembles/{ensembleId}/members/{seatId}", ensembleId, firstSeat)
                .then()
                .statusCode(200);

        given().get("/api/ensembles/{id}/members", ensembleId)
                .then()
                .statusCode(200)
                .body("findAll { it.voiceId != null }.role", everyItem(equalTo("SUBSTITUTE")))
                .body("find { it.voiceId == null }.role", equalTo("CONDUCTOR"));
    }

    @Test
    void addVoiceSeat_withoutRole_joinsTheRoleOfTheMembersOtherVoiceSeats() throws Exception {
        addSeat(seat(addVoice("1. Trumpet"), EnsembleRole.SUBSTITUTE));

        given().contentType(ContentType.JSON)
                .body(toJson(seat(addVoice("2. Trumpet"), null)))
                .post("/api/ensembles/{id}/members", ensembleId)
                .then()
                .statusCode(200)
                .body("role", equalTo("SUBSTITUTE"));

        given().get("/api/ensembles/{id}/members", ensembleId)
                .then()
                .statusCode(200)
                .body("role", everyItem(equalTo("SUBSTITUTE")));
    }

    @Test
    void updateSeat_withoutRole_keepsItsRole() throws Exception {
        String seatId = addSeat(seat(null, EnsembleRole.CONDUCTOR));

        given().contentType(ContentType.JSON)
                .body(toJson(new EnsembleMembership()))
                .put("/api/ensembles/{ensembleId}/members/{seatId}", ensembleId, seatId)
                .then()
                .statusCode(200)
                .body("role", equalTo("CONDUCTOR"));
    }

    @Test
    void deleteMember_removesFromList() throws Exception {
        String seatId = addSeat(seat(null, null));

        given().delete("/api/ensembles/{ensembleId}/members/{seatId}", ensembleId, seatId)
                .then()
                .statusCode(204);

        given().get("/api/ensembles/{id}/members", ensembleId)
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    private CreateEnsembleMembership seat(UUID voiceId, EnsembleRole role) {
        CreateEnsembleMembership seat = new CreateEnsembleMembership();
        seat.setMemberId(UUID.fromString(memberId));
        seat.setVoiceId(voiceId);
        seat.setRole(role);
        return seat;
    }

    private String addSeat(CreateEnsembleMembership seat) throws Exception {
        return given().contentType(ContentType.JSON)
                .body(toJson(seat))
                .post("/api/ensembles/{id}/members", ensembleId)
                .then()
                .statusCode(200)
                .extract()
                .path("id");
    }

    private UUID addVoice(String label) throws Exception {
        CreateEnsembleVoice voice = new CreateEnsembleVoice();
        voice.setLabel(label);
        String id = given().contentType(ContentType.JSON)
                .body(toJson(voice))
                .post("/api/ensembles/{ensembleId}/voices", ensembleId)
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        return UUID.fromString(id);
    }

    private static String toJson(Object value) throws Exception {
        try (Jsonb jsonb = JsonbBuilder.newBuilder().build()) {
            return jsonb.toJson(value);
        }
    }
}
