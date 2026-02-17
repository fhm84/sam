package de.halbmann.sam.api.entity;

import java.util.List;
import lombok.Data;

/**
 * Result of evaluating a sheet music's instrumentations against an ensemble definition. Contains an
 * overall score, a status classification, and a per-voice breakdown explaining how each voice was
 * matched.
 */
@Data
public class CoverageResult {

    /**
     * Overall coverage score (0.0 - 1.0). Computed as the weighted sum of individual voice scores
     * divided by the total voice weight.
     */
    double coverageScore;

    /**
     * High-level assessment: COMPLETE, PLAYABLE, or INCOMPLETE.
     */
    CoverageStatus status;

    /**
     * Per-voice breakdown showing which instrumentation matched each voice and why.
     */
    List<VoiceCoverageDetail> details;
}
