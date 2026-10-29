package de.halbmann.sam.business.members.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.eventlog.EventType;
import de.halbmann.sam.api.entity.members.Member;
import de.halbmann.sam.api.entity.members.MemberContact;
import de.halbmann.sam.api.entity.members.MembershipStatus;
import de.halbmann.sam.business.eventlog.boundary.EventLogRepository;
import de.halbmann.sam.business.eventlog.entity.EventLogEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Retention of member data: former members are erased after the retention period, old audit rows
 * of profiles go, a login that ends anonymises its event-log entries, and members can read their own
 * data.
 */
@QuarkusTest
class MemberRetentionTest {

    @Inject
    MemberRetention memberRetention;

    @Inject
    MemberService memberService;

    @Inject
    EventLogRepository eventLogRepository;

    @Inject
    EntityManager entityManager;

    private final List<String> createdIds = new ArrayList<>();

    @AfterEach
    void deleteCreatedMembers() {
        createdIds.forEach(id -> {
            try {
                memberService.deleteMember(id);
            } catch (RuntimeException alreadyGone) {
                // erased by the test itself
            }
        });
        createdIds.clear();
    }

    @Test
    void formerMembers_areErasedOnlyAfterTheRetentionPeriod() {
        LocalDate today = LocalDate.now();
        String expired = createMember(MembershipStatus.FORMER, today.minusMonths(13));
        String recent = createMember(MembershipStatus.FORMER, today.minusMonths(1));
        String active = createMember(MembershipStatus.ACTIVE, null);

        MemberRetention.Result result = memberRetention.apply(today);

        assertTrue(result.erasedMembers() >= 1);
        assertFalse(exists(expired), "left 13 months ago: erased");
        assertTrue(exists(recent), "left a month ago: kept");
        assertTrue(exists(active));
        assertEquals(0, count("select count(*) from members_aud where id = :id", expired), "history purged too");
    }

    @Test
    void supersededAuditRows_olderThanTheRetentionPeriod_areDeleted() {
        String id = createMember(MembershipStatus.ACTIVE, null);
        updateEmail(id, "first@example.org");
        updateEmail(id, "second@example.org");
        assertEquals(3, count("select count(*) from members_aud where id = :id", id));
        // Make the revisions that superseded the old rows look two years old
        long twoYearsAgo = LocalDate.now()
                .minusYears(2)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        QuarkusTransaction.requiringNew()
                .run(() -> entityManager
                        .createNativeQuery("update REVINFO set REVTSTMP = :ts where REV in"
                                + " (select REVEND from members_aud where id = :id and REVEND is not null)")
                        .setParameter("ts", twoYearsAgo)
                        .setParameter("id", UUID.fromString(id))
                        .executeUpdate());

        memberRetention.apply(LocalDate.now());

        assertEquals(1, count("select count(*) from members_aud where id = :id", id), "only the current row stays");
        assertEquals(1, count("select count(*) from members where id = :id and email = 'second@example.org'", id));
    }

    @Test
    void leavingTheBand_anonymisesTheEventLogOfTheLogin() {
        String userId = "leaving-user-" + UUID.randomUUID();
        String id = createMember(MembershipStatus.ACTIVE, null);
        memberService.linkUser(id, userId);
        UUID entry = QuarkusTransaction.requiringNew().call(() -> {
            EventLogEntity e = new EventLogEntity();
            e.setOccurredAt(OffsetDateTime.now());
            e.setUserId(userId);
            e.setUsername("leaving.user");
            e.setEventType(EventType.DOCUMENT_DOWNLOAD);
            eventLogRepository.persist(e);
            return e.getId();
        });

        Member update = new Member();
        update.setName("Retention Test");
        update.setStatus(MembershipStatus.FORMER);
        memberService.updateMember(id, update);

        EventLogEntity anonymised = QuarkusTransaction.requiringNew().call(() -> eventLogRepository.findById(entry));
        assertEquals(EventLogRepository.ERASED_USER_ID, anonymised.getUserId());
        assertNull(anonymised.getUsername());
        QuarkusTransaction.requiringNew().run(() -> eventLogRepository.deleteById(entry));
    }

    @Test
    @TestSecurity(
            user = "own-data-user",
            roles = {})
    @OidcSecurity(claims = @Claim(key = "sub", value = "own-data-user"))
    void ownData_showsTheCallersPersonalData() {
        String id = createMember(MembershipStatus.ACTIVE, null);
        updateEmail(id, "me@example.org");
        memberService.linkUser(id, "own-data-user");

        // A plain login sees its own contact details, unlike other members'
        given().get("/api/me/member")
                .then()
                .statusCode(200)
                .body("member.id", equalTo(id))
                .body("member.contact.email", equalTo("me@example.org"))
                .body("member.onboarding", equalTo("LINKED"))
                .body("seats", empty());
    }

    @Test
    @TestSecurity(
            user = "nobody",
            roles = {})
    @OidcSecurity(claims = @Claim(key = "sub", value = "login-without-member"))
    void ownData_withoutLinkedMember_returns404() {
        given().get("/api/me/member").then().statusCode(404);
    }

    private String createMember(MembershipStatus status, LocalDate leftAt) {
        Member member = new Member();
        member.setName("Retention Test " + UUID.randomUUID());
        member.setStatus(status);
        member.setLeftAt(leftAt);
        String id = memberService.addMember(member).getId().toString();
        createdIds.add(id);
        return id;
    }

    private void updateEmail(String id, String email) {
        Member update = new Member();
        update.setName(memberService.getMember(id).getName());
        MemberContact contact = new MemberContact();
        contact.setEmail(email);
        update.setContact(contact);
        memberService.updateMember(id, update);
    }

    private boolean exists(String id) {
        return count("select count(*) from members where id = :id", id) > 0;
    }

    private long count(String sql, String id) {
        return QuarkusTransaction.requiringNew()
                .call(() -> ((Number) entityManager
                                .createNativeQuery(sql)
                                .setParameter("id", UUID.fromString(id))
                                .getSingleResult())
                        .longValue());
    }
}
