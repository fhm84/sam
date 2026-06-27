package de.halbmann.sam.core.controller;

import java.text.Normalizer;
import java.util.Locale;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TextNormalizer {

    public static String normalize(String title) {
        if (title == null) {
            return "";
        }

        String normalized = Normalizer.normalize(title, Normalizer.Form.NFKD);

        normalized = normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\p{M}", "") // Diacritics entfernen
                .replaceAll("[^a-z0-9]+", " ") // Sonderzeichen → Space
                .trim()
                .replaceAll("\\s+", " "); // Mehrfach-Spaces

        return normalized;
    }

    public static String normalizeMusician(String composer) {
        if (composer == null || composer.isBlank()) {
            return "unknown";
        }
        return normalize(composer);
    }
}
