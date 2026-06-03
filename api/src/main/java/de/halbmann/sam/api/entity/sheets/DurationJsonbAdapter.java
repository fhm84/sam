package de.halbmann.sam.api.entity.sheets;

import jakarta.json.bind.adapter.JsonbAdapter;
import java.time.Duration;

/** Serialises {@link java.time.Duration} as an ISO-8601 string (e.g. {@code PT3M30S}) and parses it back. */
public class DurationJsonbAdapter implements JsonbAdapter<Duration, String> {

    @Override
    public String adaptToJson(Duration duration) {
        return duration == null ? null : duration.toString();
    }

    @Override
    public Duration adaptFromJson(String value) {
        return value == null ? null : Duration.parse(value);
    }
}
