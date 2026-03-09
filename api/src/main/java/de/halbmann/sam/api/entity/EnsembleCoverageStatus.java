package de.halbmann.sam.api.entity;

import java.time.Instant;
import lombok.Data;

/** Status of the coverage snapshot for an ensemble. Returned by compute and status endpoints. */
@Data
public class EnsembleCoverageStatus {

    /** Timestamp of the most recent computation, or null if never computed. */
    Instant computedAt;

    /** Number of sheets in the snapshot. */
    int sheetCount;
}
