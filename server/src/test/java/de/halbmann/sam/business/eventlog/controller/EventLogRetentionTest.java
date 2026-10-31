package de.halbmann.sam.business.eventlog.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.eventlog.EventType;
import de.halbmann.sam.business.eventlog.boundary.EventLogRepository;
import de.halbmann.sam.business.eventlog.entity.EventLogEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Event log retention and self-service access: entries older than the retention period are
 * deleted, and {@code GET /api/me/event-logs} returns only the caller's own entries.
 */
@QuarkusTest
class EventLogRetentionTest {

    private static final String ME = "event-log-me";
    private static final String OTHER = "event-log-other";

    @Inject
    EventLogRetention eventLogRetention;

    @Inject
    EventLogRepository repository;

    private final List<UUID> createdIds = new ArrayList<>();

    @AfterEach
    void deleteCreatedEntries() {
        QuarkusTransaction.requiringNew().run(() -> createdIds.forEach(repository::deleteById));
        createdIds.clear();
    }

    @Test
    void entriesOlderThanTheRetentionPeriod_areDeleted() {
        OffsetDateTime now = OffsetDateTime.now();
        UUID old = entry(OTHER, null, now.minusMonths(13));
        UUID oldShare = entry(null, UUID.randomUUID(), now.minusMonths(13));
        UUID recent = entry(OTHER, null, now.minusMonths(11));

        long deleted = eventLogRetention.apply(now);

        assertTrue(deleted >= 2);
        assertNull(find(old), "13 months old: deleted");
        assertNull(find(oldShare), "share-link entries follow the same rule");
        assertNotNull(find(recent), "11 months old: kept");
    }

    @Test
    @TestSecurity(
            user = ME,
            roles = {})
    @OidcSecurity(claims = @Claim(key = "sub", value = ME))
    void ownEntries_onlyTheCallersNewestFirst() {
        OffsetDateTime now = OffsetDateTime.now();
        UUID older = entry(ME, null, now.minusDays(2));
        UUID newer = entry(ME, null, now.minusDays(1));
        entry(OTHER, null, now);
        entry(null, UUID.randomUUID(), now);

        given().get("/api/me/event-logs")
                .then()
                .statusCode(200)
                .body("totalCount", equalTo(2))
                .body("data.id", contains(newer.toString(), older.toString()))
                .body("data.userId", everyItem(equalTo(ME)));
    }

    @Test
    @TestSecurity(
            user = ME,
            roles = {})
    @OidcSecurity(claims = @Claim(key = "sub", value = ME))
    void ownEntries_arePaged() {
        OffsetDateTime now = OffsetDateTime.now();
        for (int i = 0; i < 3; i++) {
            entry(ME, null, now.minusMinutes(i));
        }

        given().queryParam("page", 1)
                .queryParam("size", 2)
                .get("/api/me/event-logs")
                .then()
                .statusCode(200)
                .body("totalCount", equalTo(3))
                .body("data", hasSize(1));
    }

    @Test
    @TestSecurity(
            user = "no-subject",
            roles = {})
    void ownEntries_withoutUserId_isEmptyNotTheWholeLog() {
        entry(OTHER, null, OffsetDateTime.now());

        given().get("/api/me/event-logs")
                .then()
                .statusCode(200)
                .body("totalCount", equalTo(0))
                .body("data", empty());
    }

    @Test
    void ownEntries_requireALogin() {
        given().get("/api/me/event-logs").then().statusCode(401);
    }

    private UUID entry(String userId, UUID shareTokenId, OffsetDateTime occurredAt) {
        UUID id = QuarkusTransaction.requiringNew().call(() -> {
            EventLogEntity e = new EventLogEntity();
            e.setOccurredAt(occurredAt);
            e.setUserId(userId);
            e.setShareTokenId(shareTokenId);
            e.setEventType(EventType.DOCUMENT_DOWNLOAD);
            repository.persist(e);
            return e.getId();
        });
        createdIds.add(id);
        return id;
    }

    private EventLogEntity find(UUID id) {
        return QuarkusTransaction.requiringNew().call(() -> repository.findById(id));
    }
}
