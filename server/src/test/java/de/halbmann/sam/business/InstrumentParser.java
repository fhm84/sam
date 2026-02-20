package de.halbmann.sam.business;

public class InstrumentParser {

    public static ParsedResult parseVoice(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input must not be null or blank");
        }

        int firstUnderscore = input.indexOf('_');
        int lastUnderscore = input.lastIndexOf('_');

        if (firstUnderscore == -1 || firstUnderscore == lastUnderscore) {
            throw new IllegalArgumentException(
                    "Invalid format: expected {name}_{transposition}_{clef} or {part}_{name}_{transposition}_{clef}, got: "
                            + input);
        }

        String clef = input.substring(lastUnderscore + 1);
        String remaining = input.substring(0, lastUnderscore);

        int secondToLastUnderscore = remaining.lastIndexOf('_');

        String transposition = remaining.substring(secondToLastUnderscore + 1);
        remaining = remaining.substring(0, secondToLastUnderscore);

        // 'remaining' is now either "{name}" or "{part}_{name}"
        String name = remaining;

        if (name.isBlank() || transposition.isBlank() || clef.isBlank()) {
            throw new IllegalArgumentException("name, transposition and clef must not be empty, got: " + input);
        }

        return new ParsedResult(null, name, transposition, clef);
    }

    public static ParsedResult parseInstrumentation(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input must not be null or blank");
        }

        int firstUnderscore = input.indexOf('_');
        int lastUnderscore = input.lastIndexOf('_');

        if (firstUnderscore == -1 || firstUnderscore == lastUnderscore) {
            throw new IllegalArgumentException(
                    "Invalid format: expected {name}_{transposition}_{clef} or {part}_{name}_{transposition}_{clef}, got: "
                            + input);
        }

        String clef = input.substring(lastUnderscore + 1);
        String remaining = input.substring(0, lastUnderscore);

        int secondToLastUnderscore = remaining.lastIndexOf('_');

        String transposition = remaining.substring(secondToLastUnderscore + 1);
        remaining = remaining.substring(0, secondToLastUnderscore);

        // 'remaining' is now either "{name}" or "{part}_{name}"
        String part = null;
        String name;

        int partUnderscore = remaining.indexOf('_');
        if (partUnderscore == -1) {
            // no underscore left → no part, remaining is just the name
            name = remaining;
        } else {
            // first segment is part, rest is name (which may contain underscores)
            part = remaining.substring(0, partUnderscore);
            name = remaining.substring(partUnderscore + 1);
        }

        if (name.isBlank() || transposition.isBlank() || clef.isBlank()) {
            throw new IllegalArgumentException("name, transposition and clef must not be empty, got: " + input);
        }
        if (part != null && part.isBlank()) {
            throw new IllegalArgumentException("part must not be empty if present, got: " + input);
        }

        return new ParsedResult(part, name, transposition, clef);
    }
}
