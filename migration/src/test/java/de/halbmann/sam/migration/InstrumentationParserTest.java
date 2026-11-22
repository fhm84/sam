package de.halbmann.sam.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.halbmann.sam.api.entity.instruments.Clef;
import de.halbmann.sam.api.entity.instruments.InstrumentTransposing;
import de.halbmann.sam.api.entity.sheets.Instrumentation;
import de.halbmann.sam.api.entity.sheets.NotationType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InstrumentationParserTest {

    private static Instrumentation single(String part) {
        InstrumentationParser.Result result = InstrumentationParser.parse(part);
        assertEquals(List.of(), result.unparsed(), "unparsed");
        assertEquals(1, result.instrumentations().size());
        return result.instrumentations().getFirst();
    }

    @ParameterizedTest
    @CsvSource({
        "1. Trompete in b, Trompete, Bb, TROMPETE_BB, 1.",
        "2. Alt-Saxophon in es, Alt-Saxophon, Eb, ALT-SAXOPHON_EB, 2.",
        "4. Horn in es, Horn, Eb, HORN_EB, 4.",
        "1. Horn in f, Horn, F, HORN_F, 1.",
        "1. Flöte in des, Flöte, Db, FLOETE_DB, 1.",
        "1. Flöte in c, Flöte, C, FLOETE_C, 1.",
        "Schlagzeug, Schlagzeug, C, SCHLAGZEUG_C, ",
    })
    void parsesNumberNameAndKey(String part, String name, String transposition, String id, String label) {
        Instrumentation i = single(part);
        assertEquals(name, i.getInstrument().getName());
        assertEquals(
                InstrumentTransposing.valueOf(transposition), i.getInstrument().getTransposition());
        assertEquals(id, i.getInstrument().getId());
        assertEquals(label, i.getPartLabel());
    }

    @ParameterizedTest
    @CsvSource({
        "Solo-Stimme Trompete in b, Trompete, Solo",
        "1. Solo-Stimme Klarinette in b, Klarinette, 1. Solo",
        "Solo-Stimme kleine Trommel, Kleine Trommel, Solo",
        "1. Solostimme in b, Solostimme, 1.",
    })
    void parsesSoloParts(String part, String name, String label) {
        Instrumentation i = single(part);
        assertEquals(name, i.getInstrument().getName());
        assertEquals(label, i.getPartLabel());
    }

    @Test
    void parsesTwoWordNamesAndAliases() {
        assertEquals("Große Trommel", single("Große Trommel").getInstrument().getName());
        assertEquals(NotationType.PERCUSSION, single("Große Trommel").getNotationType());
        assertEquals(
                "Piccolo-Flöte", single("kleine Flöte in c").getInstrument().getName());
        assertEquals("Flöte", single("große Flöte in c").getInstrument().getName());
    }

    @ParameterizedTest
    @CsvSource({
        "1. Bass in c, BASS",
        "1. Posaune in c, BASS",
        "1. Fagott in c, BASS",
        "Pauken, BASS",
        "1. Bass in b, TREBLE",
        "1. Bass in es, TREBLE",
        "1. Bariton-Saxophon in es, TREBLE",
        "1. Posaune in b, TREBLE",
        "1. Trompete in b, TREBLE",
    })
    void transposedPartsAreTrebleAndLowConcertPitchPartsBass(String part, String clef) {
        assertEquals(Clef.valueOf(clef), single(part).getClef());
    }

    @Test
    void reportsUnparsablePartsInsteadOfDroppingThem() {
        InstrumentationParser.Result result =
                InstrumentationParser.parse("1. Trompete in b; ;1. Horn in xyz;Bass in c, Direktion");

        assertEquals(1, result.instrumentations().size());
        assertEquals(List.of("1. Horn in xyz", "Bass in c, Direktion"), result.unparsed());
    }

    @Test
    void dropsRepeatedPartsAndReportsThem() {
        InstrumentationParser.Result result =
                InstrumentationParser.parse("1. Trompete in b;2. Trompete in b;1. Trompete in b;1. Trompete in c");

        assertEquals(
                List.of("TROMPETE_BB 1.", "TROMPETE_BB 2.", "TROMPETE_C 1."),
                result.instrumentations().stream()
                        .map(i -> i.getInstrument().getId() + " " + i.getPartLabel())
                        .toList(),
                "each part once, other part number or key is another part");
        assertEquals(List.of("1. Trompete in b"), result.duplicates());
        assertEquals(List.of(), result.unparsed());
    }

    @Test
    void mapsUnexpressibleLegacyPartsAndKeepsTheirWording() {
        Instrumentation i = single("Solo Bicyle-Bell");
        assertEquals("Percussion", i.getInstrument().getName());
        assertEquals("Solo", i.getPartLabel());
        assertEquals("Solo Bicyle-Bell", i.getNotes());
    }

    @Test
    void unknownKeyIsNotGuessed() {
        assertNull(InstrumentationParser.parseTransposition("h"));
        assertTrue(
                InstrumentationParser.parse("Trompete in h").instrumentations().isEmpty());
    }
}
