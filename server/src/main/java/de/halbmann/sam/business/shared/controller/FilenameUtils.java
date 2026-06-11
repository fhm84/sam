package de.halbmann.sam.business.shared.controller;

public final class FilenameUtils {

    private FilenameUtils() {}

    public static String sanitizeFilename(String name) {
        return sanitizeFilename(name, "export");
    }

    public static String sanitizeFilename(String name, String fallback) {
        if (name == null || name.isBlank()) return fallback;
        String s = name.replace("ä", "ae")
                .replace("Ä", "Ae")
                .replace("ö", "oe")
                .replace("Ö", "Oe")
                .replace("ü", "ue")
                .replace("Ü", "Ue")
                .replace("ß", "ss");
        return s.replaceAll("[^a-zA-Z0-9._\\- ]", "_").trim();
    }
}
