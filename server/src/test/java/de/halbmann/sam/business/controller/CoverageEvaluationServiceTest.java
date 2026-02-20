package de.halbmann.sam.business.controller;

import static de.halbmann.sam.business.BusinessObjectsMother.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.halbmann.sam.api.entity.CoverageResult;
import de.halbmann.sam.api.entity.VoiceCoverageDetail;
import de.halbmann.sam.business.entity.EnsembleVoiceEntity;
import de.halbmann.sam.business.entity.SheetMusicEntity;
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
}
