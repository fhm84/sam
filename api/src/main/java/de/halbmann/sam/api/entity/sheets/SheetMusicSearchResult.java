package de.halbmann.sam.api.entity.sheets;

import de.halbmann.sam.api.entity.ensembles.CoverageSnapshotSummary;
import de.halbmann.sam.api.entity.shared.SearchResultMetrics;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Search result wrapper around {@link SheetMusic}. Adds optional full-text search metrics (rank,
 * headline) from PostgreSQL {@code tsvector} and an optional ensemble coverage summary that is
 * attached when the caller passes an {@code ensemble} query parameter.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SheetMusicSearchResult extends SheetMusic {

    SearchResultMetrics metrics;

    CoverageSnapshotSummary coverage;

    public SheetMusicSearchResult(SheetMusic sheetMusic) {
        super(
                sheetMusic.getId(),
                sheetMusic.getTitle(),
                sheetMusic.getSubtitle(),
                sheetMusic.getPublisher(),
                sheetMusic.getPublisherIpi(),
                sheetMusic.getComposer(),
                sheetMusic.getArranger(),
                sheetMusic.getOriginalBy(),
                sheetMusic.getGenre(),
                sheetMusic.getStyle(),
                sheetMusic.getDifficultyLevel(),
                sheetMusic.getDuration(),
                sheetMusic.isFavorite(),
                sheetMusic.getYearOfComposition(),
                sheetMusic.getEdition(),
                sheetMusic.getCopyright(),
                sheetMusic.getRating(),
                sheetMusic.getIswc(),
                sheetMusic.getGemaWorkNumber(),
                sheetMusic.getAdditionalNotes(),
                sheetMusic.getTags(),
                sheetMusic.getInstrumentations(),
                sheetMusic.getAttachments());
    }

    public SheetMusicSearchResult(SheetMusic sheetMusic, SearchResultMetrics metrics) {
        this(sheetMusic);
        this.metrics = metrics;
    }
}
