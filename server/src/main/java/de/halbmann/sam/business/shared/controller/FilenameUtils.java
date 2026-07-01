package de.halbmann.sam.business.shared.controller;

import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FilenameUtils {

    public static String uniqueZipName(String displayName, Set<String> used) {
        String base = displayName != null && !displayName.isBlank() ? displayName : "document";
        String candidate = base;
        int i = 1;
        while (used.contains(candidate)) {
            int dot = base.lastIndexOf('.');
            candidate = dot > 0 ? base.substring(0, dot) + " (" + i + ")" + base.substring(dot) : base + " (" + i + ")";
            i++;
        }
        used.add(candidate);
        return candidate;
    }

    public static String sanitizeFilename(String name) {
        return sanitizeFilename(name, "export");
    }

    public static String sanitizeFilename(String name, String fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
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
