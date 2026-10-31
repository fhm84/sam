package de.halbmann.sam.business.eventlog.controller;

import de.halbmann.sam.business.eventlog.boundary.EventLogRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;

/**
 * Applies the retention period of the event log (GDPR, open question #9): entries older than
 * {@code sam.event-log.retention-months} are deleted every night, share-link entries included. A
 * period of 0 turns it off.
 */
@Slf4j
@ApplicationScoped
public class EventLogRetention {

    @Inject
    EventLogRepository repository;

    @Inject
    EventLogRetentionConfig config;

    @Scheduled(cron = "{sam.event-log.retention-cron}", identity = "event-log-retention")
    void nightly() {
        long deleted = apply(OffsetDateTime.now());
        if (deleted > 0) {
            log.atInfo().log("Event log retention: deleted {} entries", deleted);
        }
    }

    /**
     * Deletes the entries that occurred more than the retention period before {@code now}; public
     * for tests and a manual run.
     *
     * @return the number of entries deleted
     */
    public long apply(final OffsetDateTime now) {
        if (config.retentionMonths() <= 0) {
            return 0;
        }
        final OffsetDateTime cutoff = now.minusMonths(config.retentionMonths());
        return QuarkusTransaction.requiringNew().call(() -> repository.deleteOlderThan(cutoff));
    }
}
