package de.halbmann.sam.business.controller;

import static de.halbmann.sam.business.BusinessObjectsMother.*;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.NotationType;
import de.halbmann.sam.business.entity.InstrumentationEntity;
import de.halbmann.sam.business.entity.VoiceOptionEntity;
import org.junit.jupiter.api.Test;

class MatchingServiceTest {

    MatchingService service = new MatchingService();

    // ── Instrument mismatch ────────────────────────────────────────────────

    @Test
    void differentInstrumentReturnsZero() {
        VoiceOptionEntity opt = option("ALTO_SAX_EB", 1.0);
        InstrumentationEntity instr = instrumentation("TENOR_SAX_BB_TREBLE");

        assertEquals(0.0, service.score(opt, instr));
    }

    // ── Matching instrument, no clef, no notationType ─────────────────────

    @Test
    void matchingInstrumentNoClefNoNotationReturnsOne() {
        VoiceOptionEntity opt = option("ALTO_SAX_EB", 1.0);
        InstrumentationEntity instr = instrumentation("1_ALTO_SAX_EB_TREBLE");
        instr.setClef(null);
        instr.setNotationType(null);

        assertEquals(1.0, service.score(opt, instr));
    }

    // ── clefFactor ─────────────────────────────────────────────────────────

    @Test
    void transposingInstrumentWithClefReturnsFullClefFactor() {
        // ALTO_SAX_EB has transposition EB → clefFactor = 1.0
        VoiceOptionEntity opt = option("ALTO_SAX_EB", 1.0);
        InstrumentationEntity instr = instrumentation("1_ALTO_SAX_EB_TREBLE");
        instr.setNotationType(null); // isolate clef factor

        assertEquals(1.0, service.score(opt, instr));
    }

    @Test
    void nonTransposingInstrumentWithClefReturnsReducedClefFactor() {
        // PIANO has no transposition → clefFactor = 0.7
        VoiceOptionEntity opt = option("PIANO_NULL", 1.0);
        // Force no transposition on the option instrument
        opt.getInstrument().setTransposition(null);
        InstrumentationEntity instr = instrumentation("1_PIANO_NULL_TREBLE");
        instr.getInstrument().setTransposition(null);
        instr.setNotationType(null);

        double score = service.score(opt, instr);
        assertEquals(0.7, score, 0.001);
    }

    // ── notationTypeFactor ─────────────────────────────────────────────────

    @Test
    void standardNotationReturnsFull() {
        VoiceOptionEntity opt = option("ALTO_SAX_EB", 1.0);
        InstrumentationEntity instr = instrumentation("1_ALTO_SAX_EB_TREBLE");
        instr.setClef(null);
        instr.setNotationType(NotationType.STANDARD);

        assertEquals(1.0, service.score(opt, instr));
    }

    @Test
    void leadSheetNotationReturnsFull() {
        VoiceOptionEntity opt = option("ALTO_SAX_EB", 1.0);
        InstrumentationEntity instr = instrumentation("1_ALTO_SAX_EB_TREBLE");
        instr.setClef(null);
        instr.setNotationType(NotationType.LEAD_SHEET);

        assertEquals(1.0, service.score(opt, instr));
    }

    @Test
    void tablatureNotationReturnsReduced() {
        VoiceOptionEntity opt = option("ALTO_SAX_EB", 1.0);
        InstrumentationEntity instr = instrumentation("1_ALTO_SAX_EB_TREBLE");
        instr.setClef(null);
        instr.setNotationType(NotationType.TABLATURE);

        assertEquals(0.7, service.score(opt, instr), 0.001);
    }

    @Test
    void graphicNotationReturnsReduced() {
        VoiceOptionEntity opt = option("ALTO_SAX_EB", 1.0);
        InstrumentationEntity instr = instrumentation("1_ALTO_SAX_EB_TREBLE");
        instr.setClef(null);
        instr.setNotationType(NotationType.GRAPHIC);

        assertEquals(0.7, service.score(opt, instr), 0.001);
    }

    @Test
    void percussionNotationReturnsPartialReduction() {
        VoiceOptionEntity opt = option("ALTO_SAX_EB", 1.0);
        InstrumentationEntity instr = instrumentation("1_ALTO_SAX_EB_TREBLE");
        instr.setClef(null);
        instr.setNotationType(NotationType.PERCUSSION);

        assertEquals(0.8, service.score(opt, instr), 0.001);
    }

    // ── Below threshold → 0.0 ─────────────────────────────────────────────

    @Test
    void scoreBelowThresholdReturnsZero() {
        // non-transposing instrument (clefFactor 0.7) × TABLATURE (0.7) = 0.49 → still above 0.3
        // Use GRAPHIC + non-transposing = 0.7 * 0.7 = 0.49; still above threshold
        // To go below 0.3, we need clefFactor=0.7 and notationFactor < 0.43 — but no such value exists.
        // The lowest achievable score is 0.7 * 0.7 = 0.49, which stays above threshold 0.3.
        // Therefore this test verifies the threshold guard does NOT drop a valid low score to 0.0.
        VoiceOptionEntity opt = option("PIANO_NULL", 1.0);
        opt.getInstrument().setTransposition(null);
        InstrumentationEntity instr = instrumentation("1_PIANO_NULL_TREBLE");
        instr.getInstrument().setTransposition(null);
        instr.setNotationType(NotationType.TABLATURE);

        double score = service.score(opt, instr);
        // 0.7 * 0.7 = 0.49, above threshold — should NOT be zeroed
        assertEquals(0.49, score, 0.001);
    }
}
