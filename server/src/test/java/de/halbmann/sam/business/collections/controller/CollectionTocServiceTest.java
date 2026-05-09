package de.halbmann.sam.business.collections.controller;

import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.collections.CollectionItem;
import de.halbmann.sam.api.entity.collections.CollectionItemType;
import de.halbmann.sam.api.entity.sheets.Genre;
import de.halbmann.sam.business.collections.controller.CollectionTocService.TocRow;
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
        assertNull(service.computeTotalDuration(List.of(sheetItem(null), sheetItem(null))));
    }

    @Test
    void totalDurationEmptyListReturnsNull() {
        assertNull(service.computeTotalDuration(List.of()));
    }

    @Test
    void totalDurationSumsNonNullDurations() {
        assertEquals(
                "3:30",
                service.computeTotalDuration(
                        List.of(sheetItem(Duration.ofMinutes(2)), sheetItem(Duration.ofSeconds(90)))));
    }

    @Test
    void totalDurationSkipsNullEntries() {
        assertEquals(
                "1:30",
                service.computeTotalDuration(
                        List.of(sheetItem(Duration.ofMinutes(1)), sheetItem(null), sheetItem(Duration.ofSeconds(30)))));
    }

    // ── toRow (SHEET) ──────────────────────────────────────────────────────

    @Test
    void toRowAllFieldsPresent() {
        CollectionItem cs = sheetItem(Duration.ofSeconds(125));
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
        assertFalse(row.isTextItem());
    }

    @Test
    void toRowNullFieldsFallBackToEmpty() {
        CollectionItem cs = new CollectionItem();
        cs.setType(CollectionItemType.SHEET);

        TocRow row = service.toRow(cs);

        assertEquals("", row.identifier());
        assertEquals("", row.title());
        assertEquals("", row.subtitle());
        assertEquals("", row.genre());
        assertEquals("", row.duration());
        assertFalse(row.isTextItem());
    }

    // ── toRow (TEXT) ───────────────────────────────────────────────────────

    @Test
    void toRowTextItemRenderedAsTextRow() {
        CollectionItem item = new CollectionItem();
        item.setType(CollectionItemType.TEXT);
        item.setIdentifier("A");
        item.setTextContent("Welcome to our concert!");

        TocRow row = service.toRow(item);

        assertEquals("A", row.identifier());
        assertTrue(row.isTextItem());
        assertEquals("Welcome to our concert!", row.subtitle());
        assertNull(row.title());
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private CollectionItem sheetItem(Duration duration) {
        CollectionItem cs = new CollectionItem();
        cs.setType(CollectionItemType.SHEET);
        cs.setDuration(duration);
        return cs;
    }
}
