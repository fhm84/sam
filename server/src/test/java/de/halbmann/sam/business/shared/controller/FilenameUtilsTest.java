package de.halbmann.sam.business.shared.controller;

import static de.halbmann.sam.business.shared.controller.FilenameUtils.sanitizeFilename;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FilenameUtilsTest {

    @Test
    void nullReturnsDefaultFallback() {
        assertEquals("export", sanitizeFilename(null));
    }

    @Test
    void blankReturnsDefaultFallback() {
        assertEquals("export", sanitizeFilename("   "));
    }

    @Test
    void nullReturnsCustomFallback() {
        assertEquals("collection", sanitizeFilename(null, "collection"));
    }

    @Test
    void replacesUmlauts() {
        assertEquals("Fruehlingsstueck", sanitizeFilename("Frühlingsstück"));
    }

    @Test
    void replacesAllUmlautVariants() {
        assertEquals("ae Ae oe Oe ue Ue ss", sanitizeFilename("ä Ä ö Ö ü Ü ß"));
    }

    @Test
    void replacesSpecialChars() {
        assertEquals("Hello_World_", sanitizeFilename("Hello/World!"));
    }

    @Test
    void allowsAlphanumericAndSafePunctuation() {
        assertEquals("my-file_v1.2 final", sanitizeFilename("my-file_v1.2 final"));
    }
}
