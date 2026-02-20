package de.halbmann.sam.business.controller;

import de.halbmann.sam.api.entity.CoverageResult;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Transactional
public class CoverageEvaluationService {

    @ConfigProperty(name = "sam.coverage.base-score", defaultValue = "0.7")
    double baseScore;

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

        return evaluate(sheet, ensemble);
    }

    CoverageResult evaluate(SheetMusicEntity sheet, EnsembleEntity ensemble) {
        List<InstrumentationEntity> instrumentations = sheet.getInstrumentations();
        List<EnsembleVoiceEntity> voices = ensemble.getVoices();

        double totalWeight = 0;
        double coveredWeight = 0;
        boolean missingRequired = false;
        List<VoiceCoverageDetail> details = new ArrayList<>();

        for (EnsembleVoiceEntity voice : voices) {
            totalWeight += voice.getWeight();

            VoiceCoverageDetail detail = new VoiceCoverageDetail();
            detail.setVoiceId(voice.getId().toString());
            detail.setVoiceLabel(voice.getLabel());
            detail.setRequired(voice.isRequired());
            detail.setWeight(voice.getWeight());
            detail.setMinCount(voice.getMinCount());
            detail.setTargetCount(voice.getTargetCount());

            double effectiveCount = 0.0;
            List<String> explanations = new ArrayList<>();

            List<VoiceOptionEntity> options =
                    voice.getOptions().isEmpty() ? List.of(VoiceOptionEntity.defaultOption()) : voice.getOptions();

            for (InstrumentationEntity instrumentation : instrumentations) {
                double bestContribution = 0.0;
                VoiceOptionEntity bestOption = null;
                double bestMatchScore = 0.0;

                for (VoiceOptionEntity option : options) {
                    double matchScore = matchingService.score(option, instrumentation);
                    if (matchScore > 0) {
                        double contribution = matchScore * option.getFactor();

                        if (contribution > bestContribution) {
                            bestContribution = contribution;
                            bestOption = option;
                            bestMatchScore = matchScore;

                            // perfekt → besser geht nicht
                            if (matchScore == 1.0 && option.getFactor() == 1.0) {
                                break;
                            }
                        }
                    }
                }

                if (bestContribution > 0) {
                    effectiveCount += bestContribution;
                    explanations.add(buildExplanation(bestOption, instrumentation, bestMatchScore));
                }
            }

            detail.setEffectiveCount(effectiveCount);

            // Pflichtprüfung (hart)
            if (voice.isRequired() && effectiveCount < voice.getMinCount()) {
                missingRequired = true;
                detail.setScore(0);
                detail.setExplanation("Required voice missing");
                details.add(detail);
                continue;
            }

            // Count-basierter Score mit Amateur-Baseline
            double countScore = 0.0;
            if (effectiveCount >= 1.0) {
                double normalized = Math.min(effectiveCount / voice.getTargetCount(), 1.0);
                countScore = baseScore + (1.0 - baseScore) * normalized;
            }

            double voiceScore = countScore * voice.getWeight();
            coveredWeight += voiceScore;

            detail.setScore(voiceScore);
            detail.setExplanation(
                    explanations.isEmpty() ? "No matching instrumentation found" : String.join("; ", explanations));

            details.add(detail);
        }

        CoverageResult result = new CoverageResult();
        result.setCoverageScore(totalWeight > 0 ? coveredWeight / totalWeight : 0);
        result.setDetails(details);
        result.setMissingRequired(missingRequired);

        return result;
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
