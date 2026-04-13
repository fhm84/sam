package de.halbmann.sam.business.shared.controller;

import static de.halbmann.sam.business.BusinessObjectsMother.*;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.sheets.NotationType;
import de.halbmann.sam.business.sheets.entity.InstrumentationEntity;
import de.halbmann.sam.business.sheets.entity.VoiceOptionEntity;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for MatchingService.score() covering instrument matching, clef factor,
 * and notation type factor branches.
 *
 * <p>Option format: {@code {name}_{transposition}_{clef}}
 * <p>Instrumentation format: {@code {part}_{name}_{transposition}_{clef}}
 * <p>Instrument ID = {@code name + "_" + transposition}
 */
class MatchingServiceTest {

    MatchingService service = new MatchingService();

    // ── Instrument mismatch ────────────────────────────────────────────────

    @Test
    void differentInstrumentReturnsZero() {
        // ALTO_SAX (ID: ALTO_SAX_EB) vs TENOR_SAX (ID: TENOR_SAX_BB)
        VoiceOptionEntity opt = option("ALTO_SAX_EB_TREBLE", 1.0);
        InstrumentationEntity instr = instrumentation("1_TENOR_SAX_BB_TREBLE");

        assertEquals(0.0, service.score(opt, instr));
    }

    // ── Matching instrument, no clef, no notationType ─────────────────────

    @Test
    void matchingInstrumentNoClefNoNotationReturnsOne() {
        VoiceOptionEntity opt = option("ALTO_SAX_EB_TREBLE", 1.0);
        InstrumentationEntity instr = instrumentation("1_ALTO_SAX_EB_TREBLE");
        instr.setClef(null);
        instr.setNotationType(null);

        assertEquals(1.0, service.score(opt, instr));
    }

    // ── clefFactor ─────────────────────────────────────────────────────────

    @Test
    void transposingInstrumentWithClefReturnsFullClefFactor() {
        // Eb transposition is non-null → clefFactor = 1.0
        VoiceOptionEntity opt = option("ALTO_SAX_EB_TREBLE", 1.0);
        InstrumentationEntity instr = instrumentation("1_ALTO_SAX_EB_TREBLE");
        instr.setNotationType(null); // isolate clef factor

        assertEquals(1.0, service.score(opt, instr));
    }

    @Test
    void nonTransposingInstrumentWithClefReturnsReducedClefFactor() {
        // "NONE" is not a valid InstrumentTransposing value → fromString returns null → clefFactor = 0.7
        VoiceOptionEntity opt = option("PIANO_NONE_TREBLE", 1.0);
        InstrumentationEntity instr = instrumentation("1_PIANO_NONE_TREBLE");
        instr.setNotationType(null); // isolate clef factor

        assertEquals(0.7, service.score(opt, instr), 0.001);
    }

    // ── notationTypeFactor ─────────────────────────────────────────────────

    @Test
    void standardNotationReturnsFull() {
        VoiceOptionEntity opt = option("ALTO_SAX_EB_TREBLE", 1.0);
        InstrumentationEntity instr = instrumentation("1_ALTO_SAX_EB_TREBLE");
        instr.setClef(null);
        instr.setNotationType(NotationType.STANDARD);

        assertEquals(1.0, service.score(opt, instr));
    }

    @Test
    void leadSheetNotationReturnsFull() {
        VoiceOptionEntity opt = option("ALTO_SAX_EB_TREBLE", 1.0);
        InstrumentationEntity instr = instrumentation("1_ALTO_SAX_EB_TREBLE");
        instr.setClef(null);
        instr.setNotationType(NotationType.LEAD_SHEET);

        assertEquals(1.0, service.score(opt, instr));
    }

    @Test
    void tablatureNotationReturnsReduced() {
        VoiceOptionEntity opt = option("ALTO_SAX_EB_TREBLE", 1.0);
        InstrumentationEntity instr = instrumentation("1_ALTO_SAX_EB_TREBLE");
        instr.setClef(null);
        instr.setNotationType(NotationType.TABLATURE);

        assertEquals(0.7, service.score(opt, instr), 0.001);
    }

    @Test
    void graphicNotationReturnsReduced() {
        VoiceOptionEntity opt = option("ALTO_SAX_EB_TREBLE", 1.0);
        InstrumentationEntity instr = instrumentation("1_ALTO_SAX_EB_TREBLE");
        instr.setClef(null);
        instr.setNotationType(NotationType.GRAPHIC);

        assertEquals(0.7, service.score(opt, instr), 0.001);
    }

    @Test
    void percussionNotationReturnsPartialReduction() {
        VoiceOptionEntity opt = option("ALTO_SAX_EB_TREBLE", 1.0);
        InstrumentationEntity instr = instrumentation("1_ALTO_SAX_EB_TREBLE");
        instr.setClef(null);
        instr.setNotationType(NotationType.PERCUSSION);

        assertEquals(0.8, service.score(opt, instr), 0.001);
    }

    // ── Combined factors still above threshold ─────────────────────────────

    @Test
    void nonTransposingWithGraphicNotationStaysAboveThreshold() {
        // clefFactor = 0.7 (non-transposing, clef present) × notationFactor = 0.7 (GRAPHIC) = 0.49
        // 0.49 > threshold(0.3) → not zeroed
        VoiceOptionEntity opt = option("PIANO_NONE_TREBLE", 1.0);
        InstrumentationEntity instr = instrumentation("1_PIANO_NONE_TREBLE");
        instr.setNotationType(NotationType.GRAPHIC);

        double score = service.score(opt, instr);
        assertEquals(0.49, score, 0.001);
    }
}
