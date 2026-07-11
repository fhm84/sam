package de.halbmann.sam.cli.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FilenameUtilsTest {

    @Test
    void sanitizeTransliteratesUmlauts() {
        assertEquals("Fluegelhorn_in_b.json", FilenameUtils.sanitize("Flügelhorn in b") + ".json");
        assertEquals("Grosse_Ouvertuere", FilenameUtils.sanitize("Große Ouvertüre"));
    }

    @Test
    void sanitizeReplacesUnsafeCharacters() {
        assertEquals("A_Banda", FilenameUtils.sanitize("A Banda").replace(' ', '_'));
        assertEquals("Salut_dAmour", FilenameUtils.sanitize("Salut d'Amour").replace(' ', '_'));
    }

    @Test
    void uniqueFilenameKeepsFirstOccurrencePlain() {
        Set<String> used = new HashSet<>();
        String name = FilenameUtils.uniqueFilename("Marsch", UUID.randomUUID(), used);

        assertEquals("Marsch.json", name);
    }

    @Test
    void uniqueFilenameSupportsStringDiscriminator() {
        Set<String> used = new HashSet<>();

        String first = FilenameUtils.uniqueFilename("TROMPETE_BB", "TROMPETE_BB", used);
        String second = FilenameUtils.uniqueFilename("Polka", "other-key", used);
        String collision = FilenameUtils.uniqueFilename("Polka", "disc", used);

        assertEquals("TROMPETE_BB.json", first);
        assertEquals("Polka.json", second);
        assertEquals("Polka-disc.json", collision);
    }

    @Test
    void uniqueFilenameDisambiguatesCollisionsById() {
        Set<String> used = new HashSet<>();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();

        String first = FilenameUtils.uniqueFilename("Marsch", firstId, used);
        String second = FilenameUtils.uniqueFilename("Marsch", secondId, used);

        assertEquals("Marsch.json", first);
        assertNotEquals(first, second);
        assertEquals("Marsch-" + secondId.toString().substring(0, 8) + ".json", second);
    }
}
