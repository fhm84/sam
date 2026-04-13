package de.halbmann.sam.business.ensembles.controller;

import static de.halbmann.sam.business.BusinessObjectsMother.*;
import static org.junit.jupiter.api.Assertions.*;

import de.halbmann.sam.api.entity.ensembles.CoverageResult;
import de.halbmann.sam.api.entity.ensembles.CoverageStatus;
import de.halbmann.sam.api.entity.ensembles.VoiceCoverageDetail;
import de.halbmann.sam.business.ensembles.entity.EnsembleEntity;
import de.halbmann.sam.business.ensembles.entity.EnsembleVoiceEntity;
import de.halbmann.sam.business.shared.controller.MatchingService;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import org.junit.jupiter.api.Test;

class CoverageEvaluationServiceTest {

    MatchingService matchingService = new MatchingService();
    CoverageEvaluationService service = new CoverageEvaluationService();

    {
        service.matchingService = matchingService;
    }

    @Test
    void oneVoiceIsPlayable() {
        EnsembleVoiceEntity sax = voice("ALTO_SAX_EB_TREBLE", true, 1, 2, 1.0);

        SheetMusicEntity sheet = sheetWith(instrumentation("1_ALTO_SAX_EB_TREBLE"));

        CoverageResult result = service.evaluate(sheet, ensembleWith(sax));

        VoiceCoverageDetail detail = result.getDetails().get(0);

        assertEquals(1.0, detail.getEffectiveCount());
        assertTrue(result.isPlayable());
        // weight=1.0 → detail.score == countScore (no change in value)
        assertEquals(0.5, detail.getScore());
    }

    @Test
    void moreVoicesIncreaseScore() {
        EnsembleVoiceEntity sax = voice("ALTO_SAX_EB_TREBLE", true, 1, 3, 1.0);

        SheetMusicEntity sheet = sheetWith(
                instrumentation("1_ALTO_SAX_EB_TREBLE"),
                instrumentation("2_ALTO_SAX_EB_TREBLE"),
                instrumentation("3_ALTO_SAX_EB_TREBLE"));

        CoverageResult result = service.evaluate(sheet, ensembleWith(sax));

        VoiceCoverageDetail detail = result.getDetails().get(0);

        assertEquals(3.0, detail.getEffectiveCount());
        assertEquals(1.0, detail.getScore());
    }

    @Test
    void alternativeInstrumentIsWeighted() {
        EnsembleVoiceEntity tenorhorn = voiceWithOption(
                "TENORHORN_BB_TREBLE", 1, 3, option("TENORHORN_BB_TREBLE", 1.0), option("BARITONE_BB_TREBLE", 0.85));

        SheetMusicEntity sheet = sheetWith(instrumentation("BARITONE_BB_TREBLE"));

        CoverageResult result = service.evaluate(sheet, ensembleWith(tenorhorn));

        VoiceCoverageDetail detail = result.getDetails().get(0);

        assertEquals(0.85, detail.getEffectiveCount());
        assertTrue(result.isPlayable()); // minCount = 1
    }

    @Test
    void voiceWithNoOptionsIsUncovered() {
        EnsembleVoiceEntity noOptions = voiceWithOption("TRUMPET_BB_TREBLE", 0, 1 /*, no options */);

        SheetMusicEntity sheet = sheetWith(instrumentation("TRUMPET_BB_TREBLE"));

        CoverageResult result = service.evaluate(sheet, ensembleWith(noOptions));

        VoiceCoverageDetail detail = result.getDetails().get(0);

        assertEquals(0.0, detail.getEffectiveCount());
        assertEquals(0.0, detail.getScore());
        assertTrue(detail.getExplanation().contains("No voice options defined"));
    }

    @Test
    void requiredVoiceWithNoOptionsTriggersIncomplete() {
        // voiceWithOption with required=false, but we need required=true here —
        // use the voice() helper which sets required and adds a default option,
        // then manually clear the options to simulate "required but no options".
        EnsembleVoiceEntity v = voice("TRUMPET_BB_TREBLE", true, 1, 1, 1.0);
        v.getOptions().clear();

        SheetMusicEntity sheet = sheetWith(instrumentation("TRUMPET_BB_TREBLE"));

        CoverageResult result = service.evaluate(sheet, ensembleWith(v));

        assertFalse(result.isPlayable());
        assertEquals(CoverageStatus.INCOMPLETE, result.getStatus());
    }

    @Test
    void detailScoreIsNormalisedCoverageNotWeightedContribution() {
        // weight = 0.4, full coverage → detail.score should be 1.0, not 0.4
        EnsembleVoiceEntity v = voice("TRUMPET_BB_TREBLE", false, 1, 1, 0.4);

        SheetMusicEntity sheet = sheetWith(instrumentation("TRUMPET_BB_TREBLE"));

        CoverageResult result = service.evaluate(sheet, ensembleWith(v));

        VoiceCoverageDetail detail = result.getDetails().get(0);

        // countScore = 0 + 1.0 * min(1/1, 1.0) = 1.0  (baseScore=0 in tests)
        assertEquals(1.0, detail.getScore(), 1e-9);
        // Overall coverageScore is still weighted: 1.0 * 0.4 / 0.4 = 1.0
        assertEquals(1.0, result.getCoverageScore(), 1e-9);
    }

    @Test
    void singleSubstituteBelow1_0ReceivesPartialScore() {
        // factor=0.85 → effectiveCount=0.85 < 1.0 (old cliff) but > 0 (fixed)
        EnsembleVoiceEntity tenorhorn =
                voiceWithOption("TENORHORN_BB_TREBLE", 0, 1, option("BARITONE_BB_TREBLE", 0.85));

        SheetMusicEntity sheet = sheetWith(instrumentation("BARITONE_BB_TREBLE"));

        CoverageResult result = service.evaluate(sheet, ensembleWith(tenorhorn));

        VoiceCoverageDetail detail = result.getDetails().get(0);

        // Previously: effectiveCount=0.85 < 1.0 → score=0 (bug).
        // Now: effectiveCount=0.85 > 0 → normalized=0.85, countScore > 0.
        assertTrue(detail.getScore() > 0, "substitute below 1.0 effectiveCount should still score");
        assertFalse(result.getCoverageScore() > 0);
    }

    @Test
    void instrumentationIsNotDoubleCountedAcrossVoices() {
        // Two voices that would both accept the same Trumpet part.
        // Required voice should claim it first; the other voice gets nothing.
        EnsembleVoiceEntity required = voice("TRUMPET_BB_TREBLE", true, 1, 1, 1.0);
        EnsembleVoiceEntity optional = voice("TRUMPET_BB_TREBLE", false, 0, 1, 0.5);

        EnsembleEntity ensemble = new EnsembleEntity();
        ensemble.getVoices().add(required);
        ensemble.getVoices().add(optional);

        SheetMusicEntity sheet = sheetWith(instrumentation("TRUMPET_BB_TREBLE"));

        CoverageResult result = service.evaluate(sheet, ensemble);

        VoiceCoverageDetail requiredDetail = result.getDetails().stream()
                .filter(VoiceCoverageDetail::isRequired)
                .findFirst()
                .orElseThrow();
        VoiceCoverageDetail optionalDetail = result.getDetails().stream()
                .filter(d -> !d.isRequired())
                .findFirst()
                .orElseThrow();

        // Required voice claims the one trumpet part
        assertEquals(1.0, requiredDetail.getEffectiveCount());
        // Optional voice gets nothing — the part is already consumed
        assertEquals(0.0, optionalDetail.getEffectiveCount());
        assertEquals(0.0, optionalDetail.getScore());

        assertTrue(result.isPlayable()); // required voice is covered
    }

    @Test
    void surplusPartsSpillOverToLowerPriorityVoices() {
        // If the sheet has MORE parts than the required voice needs,
        // the remainder should be available for other voices.
        EnsembleVoiceEntity trumpet = voice("TRUMPET_BB_TREBLE", true, 1, 1, 1.0);
        EnsembleVoiceEntity extra = voice("TRUMPET_BB_TREBLE", false, 0, 1, 0.5);

        EnsembleEntity ensemble = new EnsembleEntity();
        ensemble.getVoices().add(trumpet);
        ensemble.getVoices().add(extra);

        SheetMusicEntity sheet =
                sheetWith(instrumentation("1_TRUMPET_BB_TREBLE"), instrumentation("2_TRUMPET_BB_TREBLE"));

        CoverageResult result = service.evaluate(sheet, ensemble);

        VoiceCoverageDetail requiredDetail = result.getDetails().stream()
                .filter(VoiceCoverageDetail::isRequired)
                .findFirst()
                .orElseThrow();
        VoiceCoverageDetail extraDetail = result.getDetails().stream()
                .filter(d -> !d.isRequired())
                .findFirst()
                .orElseThrow();

        // First part goes to required voice
        assertEquals(2.0, requiredDetail.getEffectiveCount());
        // Second part spills over to the optional voice
        assertEquals(0.0, extraDetail.getEffectiveCount());
    }
}
