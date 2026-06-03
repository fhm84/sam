package de.halbmann.sam.api.entity.shared;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Diagnostic scores produced by the sheet music full-text search pipeline. Included in search
 * results to make ranking transparent and support relevance tuning.
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultMetrics {

    double ftsRank;
    double titleSimilarity;
    double composerSimilarity;
    boolean phoneticMatch;
    double finalRank;
}
