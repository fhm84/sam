package de.halbmann.sam.api.boundary;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.ensembles.CreateEnsemble;
import de.halbmann.sam.api.entity.ensembles.CreateEnsembleMembership;
import de.halbmann.sam.api.entity.members.Member;
import de.halbmann.sam.api.entity.members.MemberContact;
import de.halbmann.sam.api.entity.persons.Person;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.business.members.controller.MemberAuditPurge;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Deleting a member erases their personal data including its audit history (GDPR, Art. 17): the
 * profile, ensemble seats and their {@code _aud} rows go, and the person goes too unless it is
 * credited on a sheet.
 */
@QuarkusTest
@TestSecurity(
        user = "librarian1",
        roles = {"music_librarian"})
class MemberErasureTest {

    @Inject
    EntityManager entityManager;

    @Inject
    MemberAuditPurge memberAuditPurge;

    private final List<String> ensembleIds = new ArrayList<>();

    @AfterEach
    void deleteEnsembles() {
        ensembleIds.forEach(id -> given().delete("/api/ensembles/{id}", id));
        ensembleIds.clear();
    }

    @Test
    void deleteMember_notCredited_erasesPersonAndAllAuditRows() {
        String memberId = createMemberWithHistory();
        assertTrue(auditRows("members_aud", "id", memberId) > 0, "precondition: member history exists");
        assertTrue(auditRows("ensemble_memberships_aud", "member_id", memberId) > 0);

        given().delete("/api/members/{id}", memberId).then().statusCode(204);

        given().get("/api/members/{id}", memberId).then().statusCode(404);
        given().get("/api/persons/{id}", memberId).then().statusCode(404);
        assertEquals(0, auditRows("members_aud", "id", memberId));
        assertEquals(0, auditRows("ensemble_memberships_aud", "member_id", memberId));
        assertEquals(0, auditRows("persons_aud", "id", memberId));
    }

    @Test
    void deleteMember_creditedOnASheet_keepsThePerson() {
        String memberId = createMemberWithHistory();
        String sheetId = createSheetArrangedBy(memberId);

        given().delete("/api/members/{id}", memberId).then().statusCode(204);

        given().get("/api/members/{id}", memberId).then().statusCode(404);
        assertEquals(
                memberId,
                given().get("/api/sheets/{id}", sheetId)
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("arranger.id"));
        assertEquals(0, auditRows("members_aud", "id", memberId));
        assertTrue(auditRows("persons_aud", "id", memberId) > 0, "the credited person keeps its history");

        given().delete("/api/sheets/{id}", sheetId).then().statusCode(204);
        given().delete("/api/persons/{id}", memberId).then().statusCode(204);
    }

    @Test
    void purge_catchesUpOnHistoryLeftByAFailedPurge() {
        // Make sure a revision exists to attach the rows to
        given().delete("/api/members/{id}", createMemberWithHistory()).then().statusCode(204);
        // Audit rows of a member who no longer exists, as a failed purge would leave them
        UUID erased = UUID.randomUUID();
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery("insert into members_aud (id, REV, REVTYPE, email)"
                                + " select :id, max(REV), 0, 'left-behind@example.org' from REVINFO")
                        .setParameter("id", erased)
                        .executeUpdate());
        assertEquals(1, auditRows("members_aud", "id", erased.toString()));

        memberAuditPurge.purgeErasedMembers();

        assertEquals(0, auditRows("members_aud", "id", erased.toString()));
    }

    /** A member whose contact changed once and who has a seat in an ensemble. */
    private String createMemberWithHistory() {
        Member member = new Member();
        member.setName("Erasure Test " + UUID.randomUUID());
        String memberId = given().contentType(ContentType.JSON)
                .body(member)
                .post("/api/members")
                .then()
                .statusCode(200)
                .extract()
                .path("id");

        MemberContact contact = new MemberContact();
        contact.setEmail("leaving@example.org");
        member.setContact(contact);
        given().contentType(ContentType.JSON)
                .body(member)
                .put("/api/members/{id}", memberId)
                .then()
                .statusCode(204);

        CreateEnsemble ensemble = new CreateEnsemble();
        ensemble.setName("Erasure Test " + UUID.randomUUID());
        String ensembleId = given().contentType(ContentType.JSON)
                .body(ensemble)
                .post("/api/ensembles")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
        ensembleIds.add(ensembleId);
        CreateEnsembleMembership seat = new CreateEnsembleMembership();
        seat.setMemberId(UUID.fromString(memberId));
        given().contentType(ContentType.JSON)
                .body(seat)
                .post("/api/ensembles/{id}/members", ensembleId)
                .then()
                .statusCode(200);
        return memberId;
    }

    private String createSheetArrangedBy(String personId) {
        Person arranger = new Person();
        arranger.setId(UUID.fromString(personId));
        SheetMusic sheet = new SheetMusic();
        sheet.setTitle("Erasure Test " + UUID.randomUUID());
        sheet.setArranger(arranger);
        return given().contentType(ContentType.JSON)
                .body(sheet)
                .post("/api/sheets")
                .then()
                .statusCode(200)
                .extract()
                .path("id");
    }

    private long auditRows(String table, String column, String id) {
        return QuarkusTransaction.requiringNew()
                .call(() -> ((Number) entityManager
                                .createNativeQuery("select count(*) from " + table + " where " + column + " = :id")
                                .setParameter("id", UUID.fromString(id))
                                .getSingleResult())
                        .longValue());
    }
}
