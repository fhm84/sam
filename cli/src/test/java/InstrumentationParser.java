import de.halbmann.sam.api.entity.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstrumentationParser {

    public static List<Instrumentation> parseInstrumentations(String input) {
        List<Instrumentation> instrumentations = new ArrayList<>();

        // Split by semicolon
        String[] parts = input.split(";");

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }

            Instrumentation instr = parseInstrument(part);
            if (instr != null) {
                instrumentations.add(instr);
            }
        }

        return instrumentations;
    }

    private static Instrumentation parseInstrument(String part) {
        // Pattern to match: "1. Instrument in key" or just "Instrument"
        Pattern pattern = Pattern.compile("^(?:(\\d+)\\.\\s+)?(.+?)(?:\\s+in\\s+([a-z]+))?$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(part);

        if (!matcher.matches()) {
            return null;
        }

        String partLabel = matcher.group(1); // e.g., "1", "2", "3"
        String instrumentName = matcher.group(2); // e.g., "Trompete", "Flöte"
        String key = matcher.group(3); // e.g., "b", "c", "es", "f"

        // Determine transposition
        InstrumentTransposing transposition = parseTransposition(key);

        // Determine clef and notation type based on instrument
        Clef clef = determineClef(instrumentName, key);
        NotationType notationType = determineNotationType(instrumentName);

        // Build the Instrument reference
        String trimmedName = instrumentName.trim();
        String instrumentId =
                DataConversion.cleanUp(trimmedName) + "_" + (transposition != null ? transposition.name() : "C");

        Instrument instrument = new Instrument();
        instrument.setId(instrumentId.toUpperCase());
        instrument.setName(trimmedName);
        instrument.setTransposition(transposition);

        Instrumentation instrumentation = new Instrumentation();
        instrumentation.setInstrument(instrument);
        instrumentation.setNotationType(notationType);
        instrumentation.setPartLabel(partLabel);
        instrumentation.setClef(clef);
        return instrumentation;
    }

    private static InstrumentTransposing parseTransposition(String key) {
        if (key == null) {
            return InstrumentTransposing.C;
        }

        switch (key.toLowerCase()) {
            case "c":
                return InstrumentTransposing.C;
            case "b":
                return InstrumentTransposing.Bb;
            case "es":
                return InstrumentTransposing.Eb;
            case "f":
                return InstrumentTransposing.F;
            case "g":
                return InstrumentTransposing.G;
            case "d":
                return InstrumentTransposing.D;
            case "a":
                return InstrumentTransposing.A;
            case "as":
            case "ab":
                return InstrumentTransposing.Ab;
            default:
                return InstrumentTransposing.C;
        }
    }

    private static Clef determineClef(String instrumentName, String key) {
        String lower = instrumentName.toLowerCase();

        // Bass clef instruments
        if (lower.contains("bass") || lower.contains("posaune") || lower.contains("bariton")) {
            // Exception: in Bb they often use treble clef in German brass bands
            if ("b".equalsIgnoreCase(key)) {
                return Clef.TREBLE;
            }
            return Clef.BASS;
        }

        // Alto clef instruments
        if (lower.contains("horn") && !"b".equalsIgnoreCase(key)) {
            // F horns typically use treble clef in modern scores,
            // but can use alto/bass. Assuming treble for simplicity.
            return Clef.TREBLE;
        }

        // Tenor clef instruments
        if (lower.contains("tenorhorn")) {
            return Clef.TREBLE; // Tenorhorn typically uses treble clef
        }

        // Everything else defaults to treble
        return Clef.TREBLE;
    }

    private static NotationType determineNotationType(String instrumentName) {
        String lower = instrumentName.toLowerCase();

        if (lower.contains("schlagzeug") || lower.contains("pauken") || lower.contains("glocken")) {
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
