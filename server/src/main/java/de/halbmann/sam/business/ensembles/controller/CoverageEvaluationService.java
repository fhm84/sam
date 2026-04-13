package de.halbmann.sam.business.ensembles.controller;

import de.halbmann.sam.api.entity.ensembles.CoverageResult;
import de.halbmann.sam.api.entity.ensembles.VoiceCoverageDetail;
import de.halbmann.sam.business.ensembles.boundary.EnsembleRepository;
import de.halbmann.sam.business.ensembles.entity.EnsembleEntity;
import de.halbmann.sam.business.ensembles.entity.EnsembleVoiceEntity;
import de.halbmann.sam.business.shared.controller.MatchingService;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.entity.InstrumentationEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.business.sheets.entity.VoiceOptionEntity;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;
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

        // Pre-build detail stubs in original voice order so the output order is stable
        // regardless of the processing order we use below.
        Map<EnsembleVoiceEntity, VoiceCoverageDetail> detailMap = new LinkedHashMap<>();
        for (EnsembleVoiceEntity voice : voices) {
            totalWeight += voice.getWeight();

            VoiceCoverageDetail detail = new VoiceCoverageDetail();
            detail.setVoiceId(voice.getId().toString());
            detail.setVoiceLabel(voice.getLabel());
            detail.setRequired(voice.isRequired());
            detail.setWeight(voice.getWeight());
            detail.setMinCount(voice.getMinCount());
            detail.setTargetCount(voice.getTargetCount());
            detailMap.put(voice, detail);
        }

        // Process voices in priority order: required voices first, then by weight descending.
        // This ensures that important seats get first pick of available instrumentations.
        List<EnsembleVoiceEntity> priorityOrder = voices.stream()
                .sorted(Comparator.comparing((EnsembleVoiceEntity v) -> v.isRequired() ? 0 : 1)
                        .thenComparing(Comparator.comparing(EnsembleVoiceEntity::getWeight)
                                .reversed()))
                .collect(Collectors.toList());

        // Use identity-based set so the tracker works with entities that have no JPA-assigned id
        // (e.g. in unit tests) as well as with fully-persisted entities.
        Set<InstrumentationEntity> consumed = Collections.newSetFromMap(new IdentityHashMap<>());

        for (EnsembleVoiceEntity voice : priorityOrder) {
            VoiceCoverageDetail detail = detailMap.get(voice);

            // Fix 1: voice options are mandatory for matching.
            // A voice without options cannot be covered by any instrumentation.
            if (voice.getOptions().isEmpty()) {
                detail.setEffectiveCount(0.0);
                detail.setScore(0.0);
                detail.setExplanation("No voice options defined — cannot be matched");
                if (voice.isRequired()) {
                    missingRequired = true;
                }
                continue;
            }

            double effectiveCount = 0.0;
            List<String> explanations = new ArrayList<>();

            for (InstrumentationEntity instrumentation : instrumentations) {
                // Fix 4: each instrumentation can only contribute to one voice.
                // Higher-priority voices have already claimed whatever they matched.
                if (consumed.contains(instrumentation)) {
                    continue;
                }

                double bestContribution = 0.0;
                VoiceOptionEntity bestOption = null;
                double bestMatchScore = 0.0;

                for (VoiceOptionEntity option : voice.getOptions()) {
                    double matchScore = matchingService.score(option, instrumentation);
                    if (matchScore > 0) {
                        double contribution = matchScore * option.getFactor();
                        if (contribution > bestContribution) {
                            bestContribution = contribution;
                            bestOption = option;
                            bestMatchScore = matchScore;
                            if (matchScore == 1.0 && option.getFactor() == 1.0) {
                                break;
                            }
                        }
                    }
                }

                if (bestContribution > 0) {
                    effectiveCount += bestContribution;
                    consumed.add(instrumentation);
                    explanations.add(buildExplanation(bestOption, instrumentation, bestMatchScore));
                }
            }

            detail.setEffectiveCount(effectiveCount);

            // Hard required check: a required voice needs at least minCount effective players.
            if (voice.isRequired() && effectiveCount < voice.getMinCount()) {
                missingRequired = true;
                detail.setScore(0.0);
                detail.setExplanation("Required voice missing");
                continue;
            }

            // Fix 3: remove the cliff at effectiveCount >= 1.0.
            // Any positive effectiveCount awards credit; the normalized ratio scales it.
            double countScore = 0.0;
            if (effectiveCount > 0) {
                double normalized = Math.min(effectiveCount / voice.getTargetCount(), 1.0);
                countScore = baseScore + (1.0 - baseScore) * normalized;
            }

            coveredWeight += countScore * voice.getWeight();

            // Fix 2: store the per-voice coverage ratio (0–1), not the weighted contribution.
            // Weighted contribution is only used for the aggregate coverageScore above.
            detail.setScore(countScore);
            detail.setExplanation(
                    explanations.isEmpty() ? "No matching instrumentation found" : String.join("; ", explanations));
        }

        CoverageResult result = new CoverageResult();
        result.setCoverageScore(totalWeight > 0 ? coveredWeight / totalWeight : 0);
        result.setDetails(new ArrayList<>(detailMap.values()));
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
