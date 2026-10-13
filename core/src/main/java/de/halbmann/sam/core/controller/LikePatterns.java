package de.halbmann.sam.core.controller;

/**
 * Builds SQL/JPQL {@code LIKE} patterns from user input, so {@code %} and {@code _} in the input
 * match literally instead of acting as wildcards. Use the result together with
 * {@code ESCAPE '\'} in the query ({@link #ESCAPE_CLAUSE}).
 */
public final class LikePatterns {

    /** Clause to append after the pattern parameter, e.g. {@code "title LIKE :p" + ESCAPE_CLAUSE}. */
    public static final String ESCAPE_CLAUSE = " ESCAPE '\\'";

    private LikePatterns() {}

    /** Escapes the LIKE wildcards {@code %} and {@code _} and the escape character itself. */
    public static String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** Pattern matching values that start with {@code prefix}, taken literally. */
    public static String startsWith(String prefix) {
        return escape(prefix) + "%";
    }

    /** Pattern matching values that contain {@code part}, taken literally. */
    public static String contains(String part) {
        return "%" + escape(part) + "%";
    }
}
