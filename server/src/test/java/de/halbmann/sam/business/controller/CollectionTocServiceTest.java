package de.halbmann.sam.business.controller;

import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.CollectionSheet;
import de.halbmann.sam.api.entity.Genre;
import de.halbmann.sam.business.controller.CollectionTocService.TocRow;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class CollectionTocServiceTest {

    CollectionTocService service = new CollectionTocService();

    // ── formatDuration ─────────────────────────────────────────────────────

    @Test
    void formatDurationNullReturnsEmpty() {
        assertEquals("", service.formatDuration(null));
    }

    @Test
    void formatDurationZeroReturnsZero() {
        assertEquals("0:00", service.formatDuration(Duration.ZERO));
    }

    @Test
    void formatDurationSecondsOnly() {
        assertEquals("0:45", service.formatDuration(Duration.ofSeconds(45)));
    }

    @Test
    void formatDurationExactMinutes() {
        assertEquals("3:00", service.formatDuration(Duration.ofMinutes(3)));
    }

    @Test
    void formatDurationMinutesAndSeconds() {
        assertEquals("4:07", service.formatDuration(Duration.ofSeconds(247)));
    }

    @Test
    void formatDurationPadsSecondsWithLeadingZero() {
        assertEquals("1:05", service.formatDuration(Duration.ofSeconds(65)));
    }

    // ── computeTotalDuration ───────────────────────────────────────────────

    @Test
    void totalDurationAllNullReturnsNull() {
        assertNull(service.computeTotalDuration(List.of(sheet(null), sheet(null))));
    }

    @Test
    void totalDurationEmptyListReturnsNull() {
        assertNull(service.computeTotalDuration(List.of()));
    }

    @Test
    void totalDurationSumsNonNullDurations() {
        assertEquals(
                "3:30",
                service.computeTotalDuration(List.of(sheet(Duration.ofMinutes(2)), sheet(Duration.ofSeconds(90)))));
    }

    @Test
    void totalDurationSkipsNullEntries() {
        assertEquals(
                "1:30",
                service.computeTotalDuration(
                        List.of(sheet(Duration.ofMinutes(1)), sheet(null), sheet(Duration.ofSeconds(30)))));
    }

    // ── toRow ──────────────────────────────────────────────────────────────

    @Test
    void toRowAllFieldsPresent() {
        CollectionSheet cs = sheet(Duration.ofSeconds(125));
        cs.setIdentifier("3");
        cs.setTitle("My Song");
        cs.setSubtitle("A subtitle");
        cs.setGenre(Genre.JAZZ);

        TocRow row = service.toRow(cs);

        assertEquals("3", row.identifier());
        assertEquals("My Song", row.title());
        assertEquals("A subtitle", row.subtitle());
        assertEquals("JAZZ", row.genre());
        assertEquals("2:05", row.duration());
    }

    @Test
    void toRowNullFieldsFallBackToEmpty() {
        TocRow row = service.toRow(new CollectionSheet());

        assertEquals("", row.identifier());
        assertEquals("", row.title());
        assertEquals("", row.subtitle());
        assertEquals("", row.genre());
        assertEquals("", row.duration());
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private CollectionSheet sheet(Duration duration) {
        CollectionSheet cs = new CollectionSheet();
        cs.setDuration(duration);
        return cs;
    }
}
