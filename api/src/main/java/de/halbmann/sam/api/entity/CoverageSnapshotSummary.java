package de.halbmann.sam.api.entity;

import java.time.Instant;
import java.util.List;
import lombok.Data;

/** Precomputed coverage result for a single sheet against an ensemble, embedded in list results. */
@Data
public class CoverageSnapshotSummary {

    double coverageScore;
    CoverageStatus status;
    boolean missingRequired;
    Instant computedAt;
    List<VoiceCoverageDetail> details;
}
