package de.halbmann.sam.business.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.halbmann.sam.api.entity.CoverageResult;
import de.halbmann.sam.api.entity.CoverageSnapshotSummary;
import de.halbmann.sam.api.entity.EnsembleCoverageStatus;
import de.halbmann.sam.api.entity.VoiceCoverageDetail;
import de.halbmann.sam.business.boundary.CoverageSnapshotRepository;
import de.halbmann.sam.business.boundary.EnsembleRepository;
import de.halbmann.sam.business.boundary.SheetRepository;
import de.halbmann.sam.business.entity.CoverageSnapshotEntity;
import de.halbmann.sam.business.entity.EnsembleEntity;
import de.halbmann.sam.business.entity.SheetMusicEntity;
import de.halbmann.sam.business.exception.EntityNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@Transactional
public class CoverageSnapshotService {

    @Inject
    EnsembleRepository ensembleRepository;

    @Inject
    SheetRepository sheetRepository;

    @Inject
    CoverageEvaluationService coverageEvaluationService;

    @Inject
    CoverageSnapshotRepository coverageSnapshotRepository;

    @Inject
    ObjectMapper objectMapper;

    public EnsembleCoverageStatus compute(final String ensembleId) {
        final EnsembleEntity ensemble = ensembleRepository
                .findByIdOptional(UUID.fromString(ensembleId))
                .orElseThrow(() -> new EntityNotFoundException("Ensemble", ensembleId));

        log.atInfo().log(() -> "compute coverage snapshot for " + ensemble.getName());

        final List<SheetMusicEntity> sheets = sheetRepository.listAll();

        for (SheetMusicEntity sheet : sheets) {
            log.atInfo().log(
                    () -> "compute coverage for ensemble " + ensemble.getName() + ", sheet " + sheet.getTitle());
            CoverageResult result = coverageEvaluationService.evaluate(sheet, ensemble);
            String detailsJson = serializeDetails(result.getDetails());
            coverageSnapshotRepository.upsert(ensemble, sheet, result, detailsJson);
        }

        EnsembleCoverageStatus status = new EnsembleCoverageStatus();
        status.setSheetCount(sheets.size());
        status.setComputedAt(Instant.now());
        return status;
    }

    public Optional<EnsembleCoverageStatus> getStatus(final String ensembleId) {
        final List<CoverageSnapshotEntity> snapshots =
                coverageSnapshotRepository.findByEnsemble(UUID.fromString(ensembleId));
        if (snapshots.isEmpty()) {
            return Optional.empty();
        }
        EnsembleCoverageStatus status = new EnsembleCoverageStatus();
        status.setSheetCount(snapshots.size());
        status.setComputedAt(snapshots.stream()
                .map(CoverageSnapshotEntity::getLastUpdate)
                .map(lastUpdate -> lastUpdate.toInstant(ZoneOffset.UTC))
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null));
        return Optional.of(status);
    }

    public Map<UUID, CoverageSnapshotSummary> findSummaries(UUID ensembleId, List<UUID> sheetIds) {
        return coverageSnapshotRepository.findByEnsembleAndSheets(ensembleId, sheetIds).stream()
                .collect(Collectors.toMap(e -> e.getSheet().getId(), this::toSummary));
    }

    private CoverageSnapshotSummary toSummary(CoverageSnapshotEntity entity) {
        CoverageSnapshotSummary summary = new CoverageSnapshotSummary();
        summary.setCoverageScore(entity.getScore());
        summary.setStatus(entity.getStatus());
        summary.setMissingRequired(entity.isMissingRequired());
        summary.setComputedAt(entity.getLastUpdate().toInstant(ZoneOffset.UTC));
        summary.setDetails(deserializeDetails(entity.getDetails()));
        return summary;
    }

    private String serializeDetails(List<VoiceCoverageDetail> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<VoiceCoverageDetail> deserializeDetails(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<VoiceCoverageDetail>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
