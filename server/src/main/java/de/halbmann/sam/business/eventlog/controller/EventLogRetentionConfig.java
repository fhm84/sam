package de.halbmann.sam.business.eventlog.controller;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.validation.constraints.Min;

/** Retention of the event log; validated at startup (a negative period stops the server). */
@ConfigMapping(prefix = "sam.event-log")
public interface EventLogRetentionConfig {

    /**
     * Entries older than this many months are deleted by the nightly job; 0 turns deletion off.
     * Env var {@code SAM_EVENT_LOG_RETENTION_MONTHS}.
     */
    @WithDefault("12")
    @Min(0)
    int retentionMonths();

    /**
     * When the job runs (Quartz cron, read by {@code @Scheduled} in {@link EventLogRetention}).
     * Env var {@code SAM_EVENT_LOG_RETENTION_CRON}.
     */
    @WithDefault("0 15 3 * * ?")
    String retentionCron();
}
