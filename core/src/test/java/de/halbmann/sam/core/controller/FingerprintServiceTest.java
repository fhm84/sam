package de.halbmann.sam.core.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FingerprintServiceTest {

    @Test
    void deterministicForSameInputs() {
        String a = FingerprintService.sheetFingerprint("Star Wars", "John Williams", "Fox", null);
        String b = FingerprintService.sheetFingerprint("Star Wars", "John Williams", "Fox", null);
        assertEquals(a, b);
    }

    @Test
    void differentComposersDifferentFingerprints() {
        String a = FingerprintService.sheetFingerprint("Title", "Composer A", null, null);
        String b = FingerprintService.sheetFingerprint("Title", "Composer B", null, null);
        assertNotEquals(a, b);
    }

    @Test
    void differentTitlesDifferentFingerprints() {
        String a = FingerprintService.sheetFingerprint("Title A", "Bach", null, null);
        String b = FingerprintService.sheetFingerprint("Title B", "Bach", null, null);
        assertNotEquals(a, b);
    }

    @Test
    void nullFieldsHandledGracefully() {
        // Should not throw — null fields normalise to "unknown" or empty string
        assertDoesNotThrow(() -> FingerprintService.sheetFingerprint(null, null, null, null));
    }

    @Test
    void outputIs64HexChars() {
        String fp = FingerprintService.sheetFingerprint("Title", "Composer", null, null);
        assertTrue(fp.matches("[0-9a-f]{64}"));
    }

    @Test
    void normalizationIsCaseInsensitive() {
        String lower = FingerprintService.sheetFingerprint("für elise", "beethoven", null, null);
        String upper = FingerprintService.sheetFingerprint("Für Elise", "Beethoven", null, null);
        assertEquals(lower, upper);
    }
}
