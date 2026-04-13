package de.halbmann.sam.business.ensembles.entity;

import de.halbmann.sam.api.entity.ensembles.CoverageStatus;
import de.halbmann.sam.business.shared.entity.AbstractEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "coverage_snapshots", uniqueConstraints = @UniqueConstraint(columnNames = {"ensemble_id", "sheet_id"}))
public class CoverageSnapshotEntity extends AbstractEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ensemble_id")
    EnsembleEntity ensemble;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_id")
    SheetMusicEntity sheet;

    double score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    CoverageStatus status;

    boolean missingRequired;

    @Column(columnDefinition = "jsonb")
    String details;
}
