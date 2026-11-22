package de.halbmann.sam.migration;

import static java.util.function.Predicate.not;

import de.halbmann.sam.api.entity.instruments.Clef;
import de.halbmann.sam.api.entity.instruments.Instrument;
import de.halbmann.sam.api.entity.instruments.InstrumentTransposing;
import de.halbmann.sam.api.entity.sheets.Instrumentation;
import de.halbmann.sam.api.entity.sheets.NotationType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the legacy {@code stimmen} string ("1. Trompete in b;2. Alt-Saxophon in es;Schlagzeug")
 * into instrumentations. Parts that do not match are reported in {@link Result#unparsed()} instead
 * of being dropped, so the conversion can refuse to write incomplete data.
 */
public class InstrumentationParser {

    private static final String WORD = "[A-Za-zÄÖÜäöüß]+(?:-[A-Za-zÄÖÜäöüß]+)*";

    // Every segment is anchored by a literal (the part number's dot, "Solo-Stimme", an adjective,
    // " in ") so the pattern stays unambiguous and cannot backtrack catastrophically (ReDoS).
    private static final Pattern INSTRUMENTATION_PATTERN = Pattern.compile(
            "^\\s*(?:(?<number>\\d+)\\.\\s*)?"
                    + "(?:(?<solo>Solo-?Stimme)\\s+)?"
                    + "(?<name>(?:(?:gro(?:ß|ss)e|kleine)\\s+)?" + WORD + ")"
                    + "(?:\\s+in\\s+(?<transposition>[a-z]{1,3}))?\\s*$",
            Pattern.CASE_INSENSITIVE);

    /** Legacy names that mean another instrument (lower case). */
    private static final Map<String, String> NAME_ALIASES = Map.of(
            "kleine flöte", "Piccolo-Flöte",
            "große flöte", "Flöte",
            "grosse flöte", "Flöte");

    /**
     * Whole legacy parts the pattern cannot express (lower case), mapped to a parsable part. The
     * original wording is kept in the instrumentation's notes.
     */
    private static final Map<String, String> PART_FALLBACKS = Map.of(
            "peitsche und schellenkranz", "Percussion",
            "solo bicyle-bell", "Solo-Stimme Percussion");

    /**
     * Outcome of parsing one {@code stimmen} string.
     *
     * @param instrumentations the parts that were understood, in input order, each part once
     * @param unparsed the parts that were not understood, verbatim
     * @param duplicates parts dropped because an earlier one is the same part (same instrument,
     *     part label and clef — SAM's unique key for a sheet's instrumentations), verbatim
     */
    public record Result(List<Instrumentation> instrumentations, List<String> unparsed, List<String> duplicates) {}

    public static Result parse(String input) {
        List<Instrumentation> instrumentations = new ArrayList<>();
        List<String> unparsed = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Arrays.stream(input.split(";"))
                .map(String::trim)
                .filter(not(String::isEmpty))
                .forEach(part -> {
                    Instrumentation instrumentation = parseInstrument(part);
                    if (instrumentation == null) {
                        unparsed.add(part);
                    } else if (!seen.add(instrumentation.getInstrument().getId() + "\u0000"
                            + instrumentation.getPartLabel() + "\u0000" + instrumentation.getClef())) {
                        duplicates.add(part);
                    } else {
                        instrumentations.add(instrumentation);
                    }
                });
        return new Result(List.copyOf(instrumentations), List.copyOf(unparsed), List.copyOf(duplicates));
    }

    private static Instrumentation parseInstrument(String part) {
        String fallback = PART_FALLBACKS.get(part.toLowerCase(Locale.GERMAN));
        if (fallback != null) {
            Instrumentation instrumentation = parseInstrument(fallback);
            instrumentation.setNotes(part);
            return instrumentation;
        }

        Matcher matcher = INSTRUMENTATION_PATTERN.matcher(part);
        if (!matcher.matches()) {
            return null;
        }

        String key = matcher.group("transposition"); // e.g. "b", "c", "es", "des"
        InstrumentTransposing transposition = parseTransposition(key);
        if (transposition == null) {
            return null; // unknown key: report it rather than guess
        }

        String instrumentName = normalizeName(matcher.group("name"));
        Clef clef = determineClef(instrumentName, key);
        NotationType notationType = determineNotationType(instrumentName);

        Instrument instrument = new Instrument();
        instrument.setId((FilenameUtils.cleanUp(instrumentName) + "_" + transposition.name()).toUpperCase(Locale.ROOT));
        instrument.setName(instrumentName);
        instrument.setTransposition(transposition);

        Instrumentation instrumentation = new Instrumentation();
        instrumentation.setInstrument(instrument);
        instrumentation.setNotationType(notationType);
        instrumentation.setPartLabel(partLabel(matcher.group("number"), matcher.group("solo") != null));
        instrumentation.setClef(clef);
        return instrumentation;
    }

    /** "1." / "1. Solo" / "Solo" / null — the number keeps its dot, as in the earlier output. */
    private static String partLabel(String number, boolean solo) {
        if (number == null) {
            return solo ? "Solo" : null;
        }
        return solo ? number + ". Solo" : number + ".";
    }

    private static String normalizeName(String rawName) {
        String name = rawName.trim().replaceAll("\\s+", " ");
        String alias = NAME_ALIASES.get(name.toLowerCase(Locale.GERMAN));
        if (alias != null) {
            return alias;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    static InstrumentTransposing parseTransposition(String key) {
        if (key == null) {
            return InstrumentTransposing.C;
        }
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "c" -> InstrumentTransposing.C;
            case "des" -> InstrumentTransposing.Db;
            case "d" -> InstrumentTransposing.D;
            case "es" -> InstrumentTransposing.Eb;
            case "f" -> InstrumentTransposing.F;
            case "g" -> InstrumentTransposing.G;
            case "a" -> InstrumentTransposing.A;
            case "as", "ab" -> InstrumentTransposing.Ab;
            case "b" -> InstrumentTransposing.Bb;
            default -> null;
        };
    }

    private static Clef determineClef(String instrumentName, String key) {
        // Transposed parts (in B♭, E♭, F, ...) are written in treble clef in German wind band
        // editions — also for basses, trombones, baritones and baritone saxophones.
        if (key != null && !"c".equalsIgnoreCase(key)) {
            return Clef.TREBLE;
        }

        String lower = instrumentName.toLowerCase(Locale.GERMAN);
        if (lower.contains("bass")
                || lower.contains("posaune")
                || lower.contains("bariton")
                || lower.contains("fagott")
                || lower.contains("pauken")
                || lower.contains("euphonium")
                || lower.contains("tuba")) {
            return Clef.BASS;
        }

        // Everything else in concert pitch defaults to treble
        return Clef.TREBLE;
    }

    private static NotationType determineNotationType(String instrumentName) {
        String lower = instrumentName.toLowerCase(Locale.GERMAN);

        if (lower.contains("schlagzeug")
                || lower.contains("pauken")
                || lower.contains("glocken")
                || lower.contains("trommel")) {
            return NotationType.PERCUSSION;
        }

        if (lower.contains("gitarre")) {
            return NotationType.TABLATURE;
        }

        if (lower.contains("gesang")) {
            return NotationType.LEAD_SHEET;
        }

        return NotationType.STANDARD;
    }
}
