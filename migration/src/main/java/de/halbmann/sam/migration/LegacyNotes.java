package de.halbmann.sam.migration;

import java.util.regex.Pattern;

/**
 * Cleans the legacy {@code sonstiges} (notes) text.
 *
 * <ul>
 *   <li>The legacy system stored line breaks as the literal characters {@code \r\n} (or
 *       {@code <br>}); they become real line breaks.
 *   <li>The "es fehlen: …" list is removed. It only compared the parts on hand with the band's
 *       standard set, which SAM's coverage evaluation derives from the instrumentations against
 *       the ensemble definition — a stored copy would go stale as soon as a part is added. The
 *       list is always the last item of a note.
 * </ul>
 */
public final class LegacyNotes {

    private static final String MISSING_PARTS_MARKER = "es fehlen:";
    private static final Pattern LINE_BREAKS = Pattern.compile("\\\\r\\\\n|\\\\n|\\r\\n|<br\\s*/?>");
    private static final Pattern TRAILING_SEPARATORS = Pattern.compile("[\\s;,]+$");

    private LegacyNotes() {}

    /** Returns the cleaned notes, or {@code null} if nothing is left. */
    public static String clean(String notes) {
        if (notes == null) {
            return null;
        }
        String text = LINE_BREAKS.matcher(notes).replaceAll("\n");
        int missing = text.indexOf(MISSING_PARTS_MARKER);
        if (missing >= 0) {
            text = text.substring(0, missing);
        }
        text = TRAILING_SEPARATORS.matcher(text).replaceAll("").strip();
        return text.isEmpty() ? null : text;
    }
}
