CREATE TABLE event_log
(
    id           UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    occurredAt   TIMESTAMPTZ NOT NULL DEFAULT now(),
    userId       TEXT,
    eventType    TEXT        NOT NULL,
    entityType   TEXT,
    entityId     UUID,
    metadata     JSONB,
    username     TEXT,
    shareTokenId UUID
);

CREATE INDEX idx_event_log_occurredAt ON event_log (occurredAt DESC);
CREATE INDEX idx_event_log_eventType ON event_log (eventType);
CREATE INDEX idx_event_log_entity ON event_log (entityType, entityId);
CREATE INDEX idx_event_log_shareTokenId ON event_log (shareTokenId);