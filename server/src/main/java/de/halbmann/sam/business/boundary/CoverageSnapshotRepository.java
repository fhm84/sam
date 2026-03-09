package de.halbmann.sam.business.boundary;

import de.halbmann.sam.api.entity.CoverageResult;
import de.halbmann.sam.business.entity.CoverageSnapshotEntity;
import de.halbmann.sam.business.entity.EnsembleEntity;
import de.halbmann.sam.business.entity.SheetMusicEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class CoverageSnapshotRepository implements PanacheRepositoryBase<CoverageSnapshotEntity, UUID> {

    public void upsert(EnsembleEntity ensemble, SheetMusicEntity sheet, CoverageResult result, String detailsJson) {
        getEntityManager()
                .createNativeQuery("INSERT INTO coverage_snapshots"
                        + " (id, version, created, lastUpdate, ensemble_id, sheet_id, score, status, missingRequired, details)"
                        + " VALUES (gen_random_uuid(), 0, now(), now(), :ensembleId, :sheetId, :score, :status, :missingRequired, cast(:details as jsonb))"
                        + " ON CONFLICT (ensemble_id, sheet_id) DO UPDATE SET"
                        + "  score = EXCLUDED.score,"
                        + "  status = EXCLUDED.status,"
                        + "  missingRequired = EXCLUDED.missingRequired,"
                        + "  details = EXCLUDED.details,"
                        + "  lastUpdate = now(),"
                        + "  version = coverage_snapshots.version + 1")
                .setParameter("ensembleId", ensemble.getId())
                .setParameter("sheetId", sheet.getId())
                .setParameter("score", result.getCoverageScore())
                .setParameter("status", result.getStatus().name())
                .setParameter("missingRequired", result.isMissingRequired())
                .setParameter("details", detailsJson)
                .executeUpdate();
    }

    public List<CoverageSnapshotEntity> findByEnsemble(UUID ensembleId) {
        return list("ensemble.id", ensembleId);
    }

    public List<CoverageSnapshotEntity> findByEnsembleAndSheets(UUID ensembleId, List<UUID> sheetIds) {
        if (sheetIds.isEmpty()) {
            return List.of();
        }
        return list("ensemble.id = ?1 and sheet.id in ?2", ensembleId, sheetIds);
    }

    public Optional<CoverageSnapshotEntity> findByEnsembleAndSheet(UUID ensembleId, UUID sheetId) {
        return find("ensemble.id = ?1 and sheet.id = ?2", ensembleId, sheetId).firstResultOptional();
    }

    public void deleteByEnsemble(UUID ensembleId) {
        delete("ensemble.id", ensembleId);
    }
}
