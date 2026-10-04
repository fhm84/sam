package de.halbmann.sam.classification.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** IDs for instruments created during classification apply. */
class InstrumentSlugTest {

    @ParameterizedTest
    @CsvSource({
        "Flügelhorn, flugelhorn",
        "Tenorhorn in B♭, tenorhorn-in-b",
        "Horn in F (1), horn-in-f-1",
        "  Große Trommel  , grosse-trommel",
        "-Tuba-, tuba",
        "Ü, u",
        "♭♯, instrument",
    })
    void slugIsReadableAsciiAndNeverEmpty(String name, String expected) {
        assertEquals(expected, DocumentClassificationService.instrumentSlug(name));
    }
}
