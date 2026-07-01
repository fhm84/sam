package de.halbmann.sam.core.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TextNormalizerTest {

    @Test
    void nullReturnsEmptyString() {
        assertEquals("", TextNormalizer.normalize(null));
    }

    @Test
    void emptyStringReturnsEmpty() {
        assertEquals("", TextNormalizer.normalize(""));
    }

    @Test
    void diacriticsStripped() {
        assertEquals("fur elise", TextNormalizer.normalize("Für Elise"));
    }

    @Test
    void uppercaseFolded() {
        assertEquals("hello world", TextNormalizer.normalize("Hello World"));
    }

    @Test
    void specialCharsReplacedWithSpace() {
        assertEquals("a b c", TextNormalizer.normalize("a-b_c"));
    }

    @Test
    void multipleSpacesCollapsed() {
        assertEquals("hello world", TextNormalizer.normalize("  Hello   World  "));
    }

    @Test
    void normalizeMusicianNullReturnsUnknown() {
        assertEquals("unknown", TextNormalizer.normalizeMusician(null));
    }

    @Test
    void normalizeMusicianBlankReturnsUnknown() {
        assertEquals("unknown", TextNormalizer.normalizeMusician("   "));
    }

    @Test
    void normalizeMusicianAppliesNormalize() {
        assertEquals("bach", TextNormalizer.normalizeMusician("Bach"));
    }
}
