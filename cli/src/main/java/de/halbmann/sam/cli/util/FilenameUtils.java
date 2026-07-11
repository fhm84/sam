package de.halbmann.sam.cli.util;

import java.util.Set;
import java.util.UUID;

public class FilenameUtils {

    private FilenameUtils() {}

    /**
     * Cleans up a title so it is safe to use as a filename: transliterates German umlauts/ß and a
     * handful of common accented Latin characters, then replaces anything else non-ASCII-safe with
     * an underscore.
     */
    public static String sanitize(final String input) {
        return input.replace("ü", "ue")
                .replace("ö", "oe")
                .replace("ä", "ae")
                .replace("ß", "ss")
                .replaceAll("Ü(?=[a-zäöüß ])", "Ue")
                .replaceAll("Ö(?=[a-zäöüß ])", "Oe")
                .replaceAll("Ä(?=[a-zäöüß ])", "Ae")
                .replace("Ü", "UE")
                .replace("Ö", "OE")
                .replace("Ä", "AE")
                .replace("'", "")
                .replaceAll("[^a-zA-Z0-9-_.]", "_");
    }

    /**
     * Builds a filename for the given title that is unique within {@code usedNames}, disambiguating
     * collisions with the given discriminator rather than failing or overwriting another record's
     * file.
     */
    public static String uniqueFilename(final String title, final String discriminator, final Set<String> usedNames) {
        String base = sanitize(title);
        String candidate = base + ".json";
        if (usedNames.add(candidate)) {
            return candidate;
        }
        candidate = base + "-" + sanitize(discriminator) + ".json";
        usedNames.add(candidate);
        return candidate;
    }

    /** UUID convenience: uses the first 8 characters of the UUID as discriminator. */
    public static String uniqueFilename(final String title, final UUID id, final Set<String> usedNames) {
        return uniqueFilename(title, id.toString().substring(0, 8), usedNames);
    }
}
