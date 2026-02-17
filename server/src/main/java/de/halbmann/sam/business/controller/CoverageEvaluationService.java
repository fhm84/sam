package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.CoverageResult;
import de.halbmann.sam.api.entity.CoverageStatus;
import de.halbmann.sam.api.entity.VoiceCoverageDetail;
import de.halbmann.sam.business.boundary.EnsembleRepository;
import de.halbmann.sam.business.boundary.SheetRepository;
import de.halbmann.sam.business.entity.*;
import de.halbmann.sam.business.exception.EntityNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class CoverageEvaluationService {

    @Inject
    SheetRepository sheetRepository;

    @Inject
    EnsembleRepository ensembleRepository;

    @Inject
    MatchingService matchingService;

    public CoverageResult evaluate(final String sheetId, final String ensembleId) {
        final SheetMusicEntity sheet = sheetRepository
                .findByIdOptional(UUID.fromString(sheetId))
                .orElseThrow(() -> new EntityNotFoundException("SheetMusic", sheetId));

        final EnsembleEntity ensemble = ensembleRepository
                .findByIdOptional(UUID.fromString(ensembleId))
                .orElseThrow(() -> new EntityNotFoundException("Ensemble", ensembleId));

        List<InstrumentationEntity> instrumentations = sheet.getInstrumentations();
        List<EnsembleVoiceEntity> voices = ensemble.getVoices();

        double totalWeight = 0;
        double coveredWeight = 0;
        boolean missingRequired = false;
        List<VoiceCoverageDetail> details = new ArrayList<>();

        for (EnsembleVoiceEntity voice : voices) {
            totalWeight += voice.getWeight();

            VoiceCoverageDetail detail = new VoiceCoverageDetail();
            detail.setVoiceLabel(voice.getLabel());
            detail.setRequired(voice.isRequired());
            detail.setWeight(voice.getWeight());

            // Find best match across all voice options and instrumentations
            double bestScore = 0;
            double bestFactor = 0;
            String bestInstrumentId = null;
            String bestExplanation = "No matching instrumentation found";

            for (VoiceOptionEntity option : voice.getOptions()) {
                for (InstrumentationEntity instrumentation : instrumentations) {
                    double matchScore = matchingService.score(option, instrumentation);
                    double effectiveScore = matchScore * option.getFactor();

                    if (effectiveScore > bestScore * bestFactor || (bestInstrumentId == null && matchScore > 0)) {
                        if (matchScore * option.getFactor() > bestScore * bestFactor) {
                            bestScore = matchScore;
                            bestFactor = option.getFactor();
                            bestInstrumentId = instrumentation.getInstrument().getId();
                            bestExplanation = buildExplanation(option, instrumentation, matchScore);
                        }
                    }
                }
            }

            detail.setMatchScore(bestScore);
            detail.setOptionFactor(bestFactor);
            detail.setMatchedInstrumentId(bestInstrumentId);
            detail.setExplanation(bestExplanation);

            if (bestInstrumentId != null) {
                coveredWeight += voice.getWeight() * bestFactor * bestScore;
            } else if (voice.isRequired()) {
                missingRequired = true;
            }

            details.add(detail);
        }

        CoverageResult result = new CoverageResult();
        result.setCoverageScore(totalWeight > 0 ? coveredWeight / totalWeight : 0);
        result.setDetails(details);
        result.setStatus(determineCoverageStatus(result.getCoverageScore(), missingRequired));

        return result;
    }

    private CoverageStatus determineCoverageStatus(final double score, final boolean missingRequired) {
        if (missingRequired) {
            return CoverageStatus.INCOMPLETE;
        }
        if (score >= 0.9) {
            return CoverageStatus.COMPLETE;
        }
        if (score >= 0.5) {
            return CoverageStatus.PLAYABLE;
        }
        return CoverageStatus.INCOMPLETE;
    }

    private String buildExplanation(
            final VoiceOptionEntity option, final InstrumentationEntity instrumentation, final double matchScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("Matched ");
        sb.append(instrumentation.getInstrument().getId());

        if (instrumentation.getPartLabel() != null) {
            sb.append(" (").append(instrumentation.getPartLabel()).append(")");
        }

        sb.append(" via ").append(option.getType()).append(" option");
        sb.append(String.format(" [score=%.2f, factor=%.2f]", matchScore, option.getFactor()));

        return sb.toString();
    }
}
