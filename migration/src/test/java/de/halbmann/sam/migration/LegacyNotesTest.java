package de.halbmann.sam.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class LegacyNotesTest {

    @Test
    void removesMissingPartsAfterOtherNotes() {
        assertEquals(
                "Rückseite: Garten-Polka",
                LegacyNotes.clean("Rückseite: Garten-Polka;\\r\\nes fehlen: 3. Trompete in b, Direktion"));
        assertEquals(
                "Rückseite: Garten-Polka", LegacyNotes.clean("Rückseite: Garten-Polka;es fehlen: 3. Trompete in b"));
    }

    @Test
    void notesWithOnlyMissingPartsBecomeNull() {
        assertNull(LegacyNotes.clean("es fehlen: 2. Tenorhorn in b"));
        assertNull(LegacyNotes.clean("es fehlen: Direktiones fehlen: Direktion"));
        assertNull(LegacyNotes.clean(""));
        assertNull(LegacyNotes.clean(null));
    }

    @Test
    void turnsLiteralLineBreaksIntoRealOnes() {
        assertEquals(
                "Rückseite: Schnee-Party, Marsch\nAnhang mit \"Tusch\"",
                LegacyNotes.clean("Rückseite: Schnee-Party, Marsch\\r\\nAnhang mit \"Tusch\""));
        assertEquals("a\nb", LegacyNotes.clean("a<br>b"));
    }

    @Test
    void keepsOtherNotesUntouched() {
        assertEquals("Zusatzstimme: Klarinette hoch", LegacyNotes.clean("Zusatzstimme: Klarinette hoch"));
    }
}
